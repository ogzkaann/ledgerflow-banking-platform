package dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TestFundingRequest(
        @NotBlank @Pattern(
                regexp = "^(0|[1-9][0-9]{0,16})(\\.[0-9]{1,2})?$",
                message = "must be a plain decimal amount with at most two decimal places")
        String amount,

        @NotBlank @Size(max = 100) String reference) {}
