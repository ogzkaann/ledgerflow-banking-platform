package dev.oguzkaandere.ledgerflow.transfer.application.service;

import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.application.result.CreateTransferResult;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.IdempotencyConflictException;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.TransferNotFoundException;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyRecord;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferInitiatedEvent;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStateTransition;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.IdempotencyCache;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.IdempotencyRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.OutboxRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.TransferHistoryRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.TransferRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransferApplicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferApplicationService.class);

    private final TransferRepository transfers;
    private final TransferHistoryRepository history;
    private final IdempotencyRepository idempotency;
    private final OutboxRepository outbox;
    private final IdempotencyCache cache;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;
    private final MeterRegistry metrics;

    public TransferApplicationService(
            TransferRepository transfers,
            TransferHistoryRepository history,
            IdempotencyRepository idempotency,
            OutboxRepository outbox,
            IdempotencyCache cache,
            Clock clock,
            Supplier<UUID> uuidGenerator,
            MeterRegistry metrics) {
        this.transfers = transfers;
        this.history = history;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.cache = cache;
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
        this.metrics = metrics;
    }

    @Transactional
    public CreateTransferResult create(CreateTransferCommand command) {
        String scope = IdempotencyRecord.CREATE_TRANSFER_SCOPE;
        String fingerprint = RequestFingerprint.forCommand(command);

        var cached = cache.find(scope, command.idempotencyKey());
        if (cached.isPresent()) {
            requireMatchingFingerprint(cached.get().fingerprint(), fingerprint);
            Transfer transfer = getTransfer(cached.get().transferId());
            logReplay(command, transfer);
            return new CreateTransferResult(transfer, true);
        }

        idempotency.acquireCreationLock(scope, command.idempotencyKey());
        var durable = idempotency.find(scope, command.idempotencyKey());
        if (durable.isPresent()) {
            requireMatchingFingerprint(durable.get().fingerprint(), fingerprint);
            Transfer transfer = getTransfer(durable.get().transferId());
            cache.put(scope, command.idempotencyKey(), fingerprint, transfer.id());
            logReplay(command, transfer);
            return new CreateTransferResult(transfer, true);
        }

        Instant now = clock.instant();
        Transfer transfer = Transfer.pending(
                TransferId.from(uuidGenerator.get()),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.money(),
                command.reference(),
                command.correlationId(),
                now);
        TransferStateTransition initial = new TransferStateTransition(
                uuidGenerator.get(), transfer.id(), null, transfer.status(), "TRANSFER_CREATED", now, 0);
        UUID eventId = uuidGenerator.get();

        transfers.save(transfer);
        history.save(initial);
        idempotency.save(new IdempotencyRecord(
                uuidGenerator.get(), scope, command.idempotencyKey(), fingerprint, transfer.id(), 202, now));
        outbox.save(new TransferInitiatedEvent(
                eventId, transfer, now, command.correlationId().value()));

        cacheAfterCommit(scope, command, fingerprint, transfer.id());
        metrics.counter("transfer.accepted", "service", "transfer-service").increment();
        LOGGER.info(
                "transfer_accepted transferId={} correlationId={} status={} eventId={} idempotencyToken={}",
                transfer.id(),
                transfer.correlationId(),
                transfer.status(),
                eventId,
                command.idempotencyKey().safeLogToken());
        return new CreateTransferResult(transfer, false);
    }

    @Transactional(readOnly = true)
    public Transfer getTransfer(TransferId id) {
        return transfers.findById(id).orElseThrow(() -> new TransferNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public List<TransferStateTransition> getHistory(TransferId id) {
        getTransfer(id);
        return history.findByTransferIdOldestFirst(id);
    }

    private static void requireMatchingFingerprint(String existing, String requested) {
        if (!existing.equals(requested)) {
            LOGGER.warn("idempotency_conflict_rejected");
            throw new IdempotencyConflictException();
        }
    }

    private static void logReplay(CreateTransferCommand command, Transfer transfer) {
        LOGGER.info(
                "idempotent_request_replayed transferId={} correlationId={} idempotencyToken={}",
                transfer.id(),
                transfer.correlationId(),
                command.idempotencyKey().safeLogToken());
    }

    private void cacheAfterCommit(
            String scope, CreateTransferCommand command, String fingerprint, TransferId transferId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cache.put(scope, command.idempotencyKey(), fingerprint, transferId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.put(scope, command.idempotencyKey(), fingerprint, transferId);
            }
        });
    }
}
