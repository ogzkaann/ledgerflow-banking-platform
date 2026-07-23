package dev.oguzkaandere.ledgerflow.transfer.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyRecord;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferInitiatedEvent;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStateTransition;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStatus;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.WorkflowOutboxEvent;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.IdempotencyRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.OutboxRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.TransferHistoryRepository;
import dev.oguzkaandere.ledgerflow.transfer.domain.port.TransferRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
class JpaTransferPersistenceAdapter
        implements TransferRepository, TransferHistoryRepository, IdempotencyRepository, OutboxRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTransferPersistenceAdapter.class);

    private final SpringDataTransferRepository transfers;
    private final SpringDataHistoryRepository history;
    private final SpringDataIdempotencyRepository idempotency;
    private final SpringDataOutboxRepository outbox;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    JpaTransferPersistenceAdapter(
            SpringDataTransferRepository transfers,
            SpringDataHistoryRepository history,
            SpringDataIdempotencyRepository idempotency,
            SpringDataOutboxRepository outbox,
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.transfers = transfers;
        this.history = history;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity entity = transfers.findById(transfer.id().value()).orElse(null);
        if (entity == null) {
            entity = TransferJpaEntity.fromDomain(transfer);
        } else {
            entity.updateFromDomain(transfer);
        }
        return transfers.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return transfers.findById(id.value()).map(TransferJpaEntity::toDomain);
    }

    @Override
    public Optional<Transfer> findByIdForUpdate(TransferId id) {
        return transfers.findByIdForUpdate(id.value()).map(TransferJpaEntity::toDomain);
    }

    @Override
    public TransferStateTransition save(TransferStateTransition transition) {
        return history.saveAndFlush(TransferHistoryJpaEntity.fromDomain(transition))
                .toDomain();
    }

    @Override
    public List<TransferStateTransition> findByTransferIdOldestFirst(TransferId transferId) {
        return history.findByTransferIdOrderBySequenceAsc(transferId.value()).stream()
                .map(TransferHistoryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void acquireCreationLock(String scope, IdempotencyKey key) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))::text",
                String.class,
                scope,
                key.value());
    }

    @Override
    public Optional<IdempotencyRecord> find(String scope, IdempotencyKey key) {
        return idempotency.findByScopeAndIdempotencyKey(scope, key.value()).map(IdempotencyJpaEntity::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return idempotency.saveAndFlush(IdempotencyJpaEntity.fromDomain(record)).toDomain();
    }

    @Override
    public void save(TransferInitiatedEvent event) {
        outbox.saveAndFlush(OutboxEventJpaEntity.fromDomain(event, objectMapper));
        LOGGER.info(
                "outbox_event_created transferId={} correlationId={} eventId={} status=PENDING",
                event.transfer().id(),
                event.transfer().correlationId(),
                event.eventId());
    }

    @Override
    public void save(WorkflowOutboxEvent event) {
        outbox.saveAndFlush(OutboxEventJpaEntity.fromDomain(event, objectMapper));
        LOGGER.info(
                "outbox_event_created transferId={} correlationId={} eventId={} eventType={} status=PENDING",
                event.transfer().id(),
                event.transfer().correlationId(),
                event.eventId(),
                event.eventType());
    }
}

interface SpringDataTransferRepository extends JpaRepository<TransferJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select transfer from TransferJpaEntity transfer where transfer.id = :id")
    Optional<TransferJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}

interface SpringDataHistoryRepository extends JpaRepository<TransferHistoryJpaEntity, UUID> {
    List<TransferHistoryJpaEntity> findByTransferIdOrderBySequenceAsc(UUID transferId);
}

interface SpringDataIdempotencyRepository extends JpaRepository<IdempotencyJpaEntity, UUID> {
    Optional<IdempotencyJpaEntity> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);
}

interface SpringDataOutboxRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
    List<OutboxEventJpaEntity> findTop100ByStatusOrderByOccurredAtAscEventIdAsc(String status);
}

@Entity
@Table(name = "transfers")
class TransferJpaEntity {
    @Id
    UUID id;

    @Column(name = "source_account_id", nullable = false)
    UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    UUID destinationAccountId;

    @Column(nullable = false, precision = Money.PRECISION, scale = Money.SCALE)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    SupportedCurrency currency;

    @Column(nullable = false, length = 100)
    String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    TransferStatus status;

    @Column(name = "correlation_id", nullable = false, length = 100)
    String correlationId;

    @Version
    @Column(nullable = false)
    long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected TransferJpaEntity() {}

    static TransferJpaEntity fromDomain(Transfer value) {
        TransferJpaEntity entity = new TransferJpaEntity();
        entity.id = value.id().value();
        entity.sourceAccountId = value.sourceAccountId();
        entity.destinationAccountId = value.destinationAccountId();
        entity.amount = value.money().amount();
        entity.currency = value.money().currency();
        entity.reference = value.reference().value();
        entity.status = value.status();
        entity.correlationId = value.correlationId().value();
        entity.version = value.version();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }

    void updateFromDomain(Transfer value) {
        sourceAccountId = value.sourceAccountId();
        destinationAccountId = value.destinationAccountId();
        amount = value.money().amount();
        currency = value.money().currency();
        reference = value.reference().value();
        status = value.status();
        correlationId = value.correlationId().value();
        updatedAt = value.updatedAt();
    }

    Transfer toDomain() {
        return new Transfer(
                TransferId.from(id),
                sourceAccountId,
                destinationAccountId,
                new Money(amount, currency),
                new TransferReference(reference),
                status,
                new CorrelationId(correlationId),
                version,
                createdAt,
                updatedAt);
    }
}

@Entity
@Table(name = "transfer_state_history")
class TransferHistoryJpaEntity {
    @Id
    UUID id;

    @Column(name = "transfer_id", nullable = false)
    UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 24)
    TransferStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 24)
    TransferStatus toStatus;

    @Column(nullable = false, length = 100)
    String reason;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;

    @Column(nullable = false)
    long sequence;

    protected TransferHistoryJpaEntity() {}

    static TransferHistoryJpaEntity fromDomain(TransferStateTransition value) {
        TransferHistoryJpaEntity entity = new TransferHistoryJpaEntity();
        entity.id = value.id();
        entity.transferId = value.transferId().value();
        entity.fromStatus = value.fromStatus();
        entity.toStatus = value.toStatus();
        entity.reason = value.reason();
        entity.occurredAt = value.occurredAt();
        entity.sequence = value.sequence();
        return entity;
    }

    TransferStateTransition toDomain() {
        return new TransferStateTransition(
                id, TransferId.from(transferId), fromStatus, toStatus, reason, occurredAt, sequence);
    }
}

@Entity
@Table(name = "idempotency_records")
class IdempotencyJpaEntity {
    @Id
    UUID id;

    @Column(nullable = false, length = 50)
    String scope;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    String requestFingerprint;

    @Column(name = "transfer_id", nullable = false)
    UUID transferId;

    @Column(name = "http_status", nullable = false)
    int httpStatus;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected IdempotencyJpaEntity() {}

    static IdempotencyJpaEntity fromDomain(IdempotencyRecord value) {
        IdempotencyJpaEntity entity = new IdempotencyJpaEntity();
        entity.id = value.id();
        entity.scope = value.scope();
        entity.idempotencyKey = value.key().value();
        entity.requestFingerprint = value.fingerprint();
        entity.transferId = value.transferId().value();
        entity.httpStatus = value.httpStatus();
        entity.createdAt = value.createdAt();
        return entity;
    }

    IdempotencyRecord toDomain() {
        return new IdempotencyRecord(
                id,
                scope,
                new IdempotencyKey(idempotencyKey),
                requestFingerprint,
                TransferId.from(transferId),
                httpStatus,
                createdAt);
    }
}

@Entity
@Table(name = "outbox_events")
class OutboxEventJpaEntity {
    @Id
    @Column(name = "event_id")
    UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    String eventType;

    @Column(name = "event_version", nullable = false)
    int eventVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    String payload;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(name = "publish_attempt_count", nullable = false)
    int publishAttemptCount;

    protected OutboxEventJpaEntity() {}

    static OutboxEventJpaEntity fromDomain(TransferInitiatedEvent event, ObjectMapper mapper) {
        Transfer transfer = event.transfer();
        var payload = new EventEnvelope(
                event.eventId(),
                TransferInitiatedEvent.EVENT_TYPE,
                TransferInitiatedEvent.EVENT_VERSION,
                event.occurredAt(),
                transfer.correlationId().value(),
                event.causationId(),
                "transfer-service",
                new EventPayload(
                        transfer.id().value(),
                        transfer.sourceAccountId(),
                        transfer.destinationAccountId(),
                        transfer.money().canonicalAmount(),
                        transfer.money().currency().name(),
                        transfer.reference().value()));
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.eventId = event.eventId();
        entity.aggregateType = "TRANSFER";
        entity.aggregateId = transfer.id().value();
        entity.eventType = TransferInitiatedEvent.EVENT_TYPE;
        entity.eventVersion = TransferInitiatedEvent.EVENT_VERSION;
        entity.payload = mapper.writeValueAsString(payload);
        entity.status = "PENDING";
        entity.occurredAt = event.occurredAt();
        entity.publishAttemptCount = 0;
        return entity;
    }

    static OutboxEventJpaEntity fromDomain(WorkflowOutboxEvent event, ObjectMapper mapper) {
        Transfer transfer = event.transfer();
        var payload = new EventEnvelope(
                event.eventId(),
                event.eventType(),
                1,
                event.occurredAt(),
                transfer.correlationId().value(),
                event.causationId(),
                "transfer-service",
                event.payload());
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.eventId = event.eventId();
        entity.aggregateType = "TRANSFER";
        entity.aggregateId = transfer.id().value();
        entity.eventType = event.eventType();
        entity.eventVersion = 1;
        entity.payload = mapper.writeValueAsString(payload);
        entity.status = "PENDING";
        entity.occurredAt = event.occurredAt();
        entity.publishAttemptCount = 0;
        return entity;
    }

    private record EventEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            Instant occurredAt,
            String correlationId,
            String causationId,
            String producer,
            Object payload) {}

    private record EventPayload(
            UUID transferId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            String amount,
            String currency,
            String reference) {}
}
