package dev.oguzkaandere.ledgerflow.account.application.service;

import dev.oguzkaandere.ledgerflow.account.adapter.out.eventing.AccountEventStore;
import dev.oguzkaandere.ledgerflow.account.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerReference;
import dev.oguzkaandere.ledgerflow.account.domain.model.Money;
import dev.oguzkaandere.ledgerflow.account.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.account.domain.model.TransferReservation;
import dev.oguzkaandere.ledgerflow.account.domain.port.AccountRepository;
import dev.oguzkaandere.ledgerflow.account.domain.port.LedgerEntryRepository;
import dev.oguzkaandere.ledgerflow.account.domain.port.TransferReservationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountTransferWorkflowService {
    public static final String TRANSFER_INITIATED = "ledgerflow.transfer.initiated.v1";
    public static final String SETTLEMENT_REQUESTED = "ledgerflow.transfer.settlement-requested.v1";
    public static final String COMPENSATION_REQUESTED = "ledgerflow.transfer.compensation-requested.v1";
    public static final String FUNDS_RESERVED = "ledgerflow.account.funds-reserved.v1";
    public static final String RESERVATION_REJECTED = "ledgerflow.account.funds-reservation-rejected.v1";
    public static final String TRANSFER_SETTLED = "ledgerflow.account.transfer-settled.v1";
    public static final String FUNDS_RELEASED = "ledgerflow.account.funds-released.v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountTransferWorkflowService.class);

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final TransferReservationRepository reservations;
    private final AccountEventStore events;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;
    private final MeterRegistry metrics;

    public AccountTransferWorkflowService(
            AccountRepository accounts,
            LedgerEntryRepository ledger,
            TransferReservationRepository reservations,
            AccountEventStore events,
            Clock clock,
            Supplier<UUID> uuidGenerator,
            MeterRegistry metrics) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.reservations = reservations;
        this.events = events;
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
        this.metrics = metrics;
    }

    @Transactional
    public void handle(WorkflowEnvelope envelope) {
        if (events.processed(envelope.eventId())) {
            metrics.counter("kafka.consumer.duplicate", "service", "account-service")
                    .increment();
            LOGGER.info(
                    "kafka_consumer_duplicate service=account-service eventId={} eventType={} correlationId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.correlationId());
            return;
        }
        switch (envelope.eventType()) {
            case TRANSFER_INITIATED -> reserve(envelope);
            case SETTLEMENT_REQUESTED -> settle(envelope);
            case COMPENSATION_REQUESTED -> release(envelope);
            default -> throw new IllegalArgumentException("Unsupported Account Service event type");
        }
        events.markProcessed(envelope.eventId(), envelope.eventType(), clock.instant());
        metrics.counter("kafka.consumer.processed", "service", "account-service")
                .increment();
    }

    private void reserve(WorkflowEnvelope envelope) {
        UUID transferId = requiredUuid(envelope, "transferId");
        UUID sourceId = requiredUuid(envelope, "sourceAccountId");
        UUID destinationId = requiredUuid(envelope, "destinationAccountId");
        String currencyCode = requiredText(envelope, "currency");
        String reference = requiredText(envelope, "reference");
        Money amount = Money.positive(
                new BigDecimal(requiredText(envelope, "amount")), SupportedCurrency.fromCode(currencyCode));
        Instant now = clock.instant();

        Account source = accounts.findByIdForUpdate(AccountId.from(sourceId)).orElse(null);
        Account destination = accounts.findById(AccountId.from(destinationId)).orElse(null);
        String rejection = rejectionReason(source, destination, sourceId, destinationId, amount);
        if (rejection != null) {
            events.append(
                    RESERVATION_REJECTED,
                    transferId,
                    envelope.correlationId(),
                    envelope.eventId().toString(),
                    Map.of("transferId", transferId, "reason", rejection),
                    now);
            LOGGER.info(
                    "funds_reservation_rejected service=account-service transferId={} eventId={} correlationId={} reason={}",
                    transferId,
                    envelope.eventId(),
                    envelope.correlationId(),
                    rejection);
            return;
        }

        TransferReservation reservation = TransferReservation.reserve(
                uuidGenerator.get(), transferId, AccountId.from(sourceId), AccountId.from(destinationId), amount, now);
        accounts.save(source.reserve(amount, now));
        reservations.save(reservation);
        metrics.counter("reservation.created", "service", "account-service").increment();
        events.append(
                FUNDS_RESERVED,
                transferId,
                envelope.correlationId(),
                envelope.eventId().toString(),
                Map.of(
                        "transferId", transferId,
                        "reservationId", reservation.reservationId(),
                        "sourceAccountId", sourceId,
                        "destinationAccountId", destinationId,
                        "amount", amount.formattedAmount(),
                        "currency", amount.currency().name(),
                        "reference", reference),
                now);
        LOGGER.info(
                "reservation_created service=account-service transferId={} eventId={} correlationId={} amount={} currency={}",
                transferId,
                envelope.eventId(),
                envelope.correlationId(),
                amount.formattedAmount(),
                amount.currency());
    }

    private void settle(WorkflowEnvelope envelope) {
        UUID transferId = requiredUuid(envelope, "transferId");
        TransferReservation reservation = reservations
                .findByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new IllegalStateException("Reservation does not exist"));
        Instant now = clock.instant();

        List<AccountId> lockOrder = List.of(reservation.sourceAccountId(), reservation.destinationAccountId()).stream()
                .sorted(Comparator.comparing(AccountId::value))
                .toList();
        Account first = accounts.findByIdForUpdate(lockOrder.get(0))
                .orElseThrow(() -> new IllegalStateException("Settlement account does not exist"));
        Account second = accounts.findByIdForUpdate(lockOrder.get(1))
                .orElseThrow(() -> new IllegalStateException("Settlement account does not exist"));
        Account source = first.id().equals(reservation.sourceAccountId()) ? first : second;
        Account destination = first.id().equals(reservation.destinationAccountId()) ? first : second;

        LedgerReference debitReference = new LedgerReference("transfer:" + transferId + ":debit");
        LedgerReference creditReference = new LedgerReference("transfer:" + transferId + ":credit");
        Account settledSource = source.settleReserved(reservation.amount(), now);
        Account creditedDestination = destination.credit(reservation.amount(), now);
        ledger.save(LedgerEntry.debit(uuidGenerator.get(), source.id(), reservation.amount(), debitReference, now));
        ledger.save(
                LedgerEntry.credit(uuidGenerator.get(), destination.id(), reservation.amount(), creditReference, now));
        accounts.save(settledSource);
        accounts.save(creditedDestination);
        reservations.save(reservation.settle(now));
        events.append(
                TRANSFER_SETTLED,
                transferId,
                envelope.correlationId(),
                envelope.eventId().toString(),
                Map.of("transferId", transferId),
                now);
        LOGGER.info(
                "transfer_settled service=account-service transferId={} eventId={} correlationId={}",
                transferId,
                envelope.eventId(),
                envelope.correlationId());
    }

    private void release(WorkflowEnvelope envelope) {
        UUID transferId = requiredUuid(envelope, "transferId");
        TransferReservation reservation = reservations
                .findByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new IllegalStateException("Reservation does not exist"));
        Account source = accounts.findByIdForUpdate(reservation.sourceAccountId())
                .orElseThrow(() -> new IllegalStateException("Reservation source account does not exist"));
        Instant now = clock.instant();
        accounts.save(source.release(reservation.amount(), now));
        reservations.save(reservation.release(now));
        metrics.counter("reservation.released", "service", "account-service").increment();
        events.append(
                FUNDS_RELEASED,
                transferId,
                envelope.correlationId(),
                envelope.eventId().toString(),
                Map.of("transferId", transferId),
                now);
        LOGGER.info(
                "reservation_released service=account-service transferId={} eventId={} correlationId={}",
                transferId,
                envelope.eventId(),
                envelope.correlationId());
    }

    private static String rejectionReason(
            Account source, Account destination, UUID sourceId, UUID destinationId, Money amount) {
        if (source == null) {
            return "SOURCE_ACCOUNT_NOT_FOUND";
        }
        if (destination == null) {
            return "DESTINATION_ACCOUNT_NOT_FOUND";
        }
        if (sourceId.equals(destinationId)) {
            return "INVALID_ACCOUNT_PAIR";
        }
        if (!source.status().allowsMutation()) {
            return "SOURCE_ACCOUNT_INACTIVE";
        }
        if (!destination.status().allowsMutation()) {
            return "DESTINATION_ACCOUNT_INACTIVE";
        }
        if (source.currency() != amount.currency() || destination.currency() != amount.currency()) {
            return "CURRENCY_MISMATCH";
        }
        if (source.availableBalance().amount().compareTo(amount.amount()) < 0) {
            return "INSUFFICIENT_FUNDS";
        }
        return null;
    }

    private static UUID requiredUuid(WorkflowEnvelope envelope, String field) {
        return UUID.fromString(requiredText(envelope, field));
    }

    private static String requiredText(WorkflowEnvelope envelope, String field) {
        var value = envelope.payload().get(field);
        if (value == null || !value.isValueNode() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Required event payload field is missing: " + field);
        }
        return value.asText();
    }
}
