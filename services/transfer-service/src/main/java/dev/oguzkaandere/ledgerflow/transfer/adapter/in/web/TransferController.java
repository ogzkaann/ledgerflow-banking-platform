package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web;

import dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto.CreateTransferRequest;
import dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto.TransferHistoryResponse;
import dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto.TransferResponse;
import dev.oguzkaandere.ledgerflow.transfer.application.command.CreateTransferCommand;
import dev.oguzkaandere.ledgerflow.transfer.application.result.CreateTransferResult;
import dev.oguzkaandere.ledgerflow.transfer.application.service.TransferApplicationService;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.CorrelationId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.IdempotencyKey;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Money;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.SupportedCurrency;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferId;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferReference;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String REPLAY_HEADER = "Idempotency-Replayed";

    private final TransferApplicationService service;

    public TransferController(TransferApplicationService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<TransferResponse> create(
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlationId,
            @Valid @RequestBody CreateTransferRequest request) {
        IdempotencyKey key = parseHeader(() -> new IdempotencyKey(idempotencyKey));
        CorrelationId correlation = correlationId == null
                ? new CorrelationId(UUID.randomUUID().toString())
                : parseHeader(() -> new CorrelationId(correlationId));
        Money money;
        try {
            money = new Money(new BigDecimal(request.amount()), SupportedCurrency.parse(request.currency()));
        } catch (NumberFormatException exception) {
            throw new InvalidTransferException("Amount must be a decimal monetary string");
        }
        CreateTransferResult result = service.create(new CreateTransferCommand(
                request.sourceAccountId(),
                request.destinationAccountId(),
                money,
                new TransferReference(request.reference()),
                key,
                correlation));
        TransferResponse response = TransferWebMapper.toResponse(result.transfer());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/api/v1/transfers/" + result.transfer().id()));
        headers.set(CORRELATION_HEADER, result.transfer().correlationId().value());
        if (result.replayed()) {
            headers.set(REPLAY_HEADER, "true");
        }
        return new ResponseEntity<>(response, headers, HttpStatus.ACCEPTED);
    }

    @GetMapping("/{transferId}")
    TransferResponse get(@PathVariable UUID transferId) {
        return TransferWebMapper.toResponse(service.getTransfer(TransferId.from(transferId)));
    }

    @GetMapping("/{transferId}/history")
    List<TransferHistoryResponse> history(@PathVariable UUID transferId) {
        return service.getHistory(TransferId.from(transferId)).stream()
                .map(TransferWebMapper::toResponse)
                .toList();
    }

    private static <T> T parseHeader(java.util.function.Supplier<T> parser) {
        try {
            return parser.get();
        } catch (InvalidTransferException exception) {
            throw new InvalidHeaderException(exception.getMessage());
        }
    }
}
