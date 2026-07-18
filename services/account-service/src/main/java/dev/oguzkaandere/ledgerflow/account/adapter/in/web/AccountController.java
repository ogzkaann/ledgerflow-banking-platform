package dev.oguzkaandere.ledgerflow.account.adapter.in.web;

import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.AccountResponse;
import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.CreateAccountRequest;
import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.LedgerPageResponse;
import dev.oguzkaandere.ledgerflow.account.application.command.CreateAccountCommand;
import dev.oguzkaandere.ledgerflow.account.application.service.AccountApplicationService;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountApplicationService accountService;

    public AccountController(AccountApplicationService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account =
                accountService.createAccount(new CreateAccountCommand(request.ownerReference(), request.currency()));
        URI location = URI.create("/api/v1/accounts/" + account.id());
        return ResponseEntity.created(location).body(AccountWebMapper.toResponse(account));
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return AccountWebMapper.toResponse(accountService.getAccount(AccountId.from(accountId)));
    }

    @GetMapping("/{accountId}/ledger")
    public LedgerPageResponse getLedger(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return AccountWebMapper.toResponse(accountService.getLedger(AccountId.from(accountId), page, size));
    }
}
