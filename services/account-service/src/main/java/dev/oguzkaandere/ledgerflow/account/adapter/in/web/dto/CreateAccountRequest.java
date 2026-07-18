package dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String ownerReference,
        @NotBlank @Size(min = 3, max = 3) String currency) {}
