package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web;

import dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto.TransferHistoryResponse;
import dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto.TransferPageResponse;
import dev.oguzkaandere.ledgerflow.transfer.adapter.in.web.dto.TransferResponse;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.Transfer;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferPage;
import dev.oguzkaandere.ledgerflow.transfer.domain.model.TransferStateTransition;

final class TransferWebMapper {
    private TransferWebMapper() {}

    static TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.id().value(),
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.money().canonicalAmount(),
                transfer.money().currency().name(),
                transfer.reference().value(),
                transfer.status().name(),
                transfer.correlationId().value(),
                transfer.createdAt(),
                transfer.updatedAt(),
                transfer.version());
    }

    static TransferPageResponse toResponse(TransferPage page) {
        return new TransferPageResponse(
                page.content().stream().map(TransferWebMapper::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    static TransferHistoryResponse toResponse(TransferStateTransition transition) {
        return new TransferHistoryResponse(
                transition.id(),
                transition.fromStatus() == null ? null : transition.fromStatus().name(),
                transition.toStatus().name(),
                transition.reason(),
                transition.occurredAt(),
                transition.sequence());
    }
}
