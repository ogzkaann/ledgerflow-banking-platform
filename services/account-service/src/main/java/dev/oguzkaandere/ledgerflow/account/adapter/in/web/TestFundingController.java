package dev.oguzkaandere.ledgerflow.account.adapter.in.web;

import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.FundingResponse;
import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.TestFundingRequest;
import dev.oguzkaandere.ledgerflow.account.application.command.FundAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.service.AccountApplicationService;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "test"})
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/test-funding")
public class TestFundingController {

    private final AccountApplicationService accountService;

    public TestFundingController(AccountApplicationService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FundingResponse addFunding(@PathVariable UUID accountId, @Valid @RequestBody TestFundingRequest request) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(request.amount());
        } catch (NumberFormatException exception) {
            throw new InvalidRequestException("Amount must be a decimal number");
        }
        return AccountWebMapper.toResponse(accountService.addTestFunding(
                new FundAccountCommand(AccountId.from(accountId), amount, request.reference())));
    }
}
