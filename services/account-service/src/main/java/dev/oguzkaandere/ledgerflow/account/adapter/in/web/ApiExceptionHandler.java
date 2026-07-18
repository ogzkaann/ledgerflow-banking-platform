package dev.oguzkaandere.ledgerflow.account.adapter.in.web;

import dev.oguzkaandere.ledgerflow.account.domain.exception.AccountNotFoundException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.AccountStateException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.CurrencyMismatchException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.DuplicateLedgerReferenceException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.InvalidMoneyException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.ReconciliationException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.UnsupportedCurrencyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBodyValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, "Request validation failed", "One or more request fields are invalid", request);
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> value = new LinkedHashMap<>();
                    value.put("field", error.getField());
                    value.put("message", error.getDefaultMessage());
                    return value;
                })
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    ProblemDetail handleParameterValidation(Exception exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "One or more request parameters are invalid",
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String detail = "Path or query parameter '%s' has an invalid value".formatted(exception.getName());
        return problem(HttpStatus.BAD_REQUEST, "Malformed request parameter", detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body", "Request body is not valid JSON", request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleNotFound(AccountNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", exception.getMessage(), request);
    }

    @ExceptionHandler({DuplicateLedgerReferenceException.class, AccountStateException.class})
    ProblemDetail handleConflict(RuntimeException exception, HttpServletRequest request) {
        String title = exception instanceof DuplicateLedgerReferenceException
                ? "Duplicate funding reference"
                : "Account state conflict";
        return problem(HttpStatus.CONFLICT, title, exception.getMessage(), request);
    }

    @ExceptionHandler({UnsupportedCurrencyException.class, CurrencyMismatchException.class})
    ProblemDetail handleUnprocessableCurrency(RuntimeException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unsupported currency", exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidMoneyException.class, InvalidRequestException.class, IllegalArgumentException.class})
    ProblemDetail handleBadRequest(RuntimeException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), request);
    }

    @ExceptionHandler(ReconciliationException.class)
    ProblemDetail handleReconciliationFailure(ReconciliationException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Account reconciliation failed", exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error(
                "unexpected_api_error path={} exceptionType={}",
                request.getRequestURI(),
                exception.getClass().getSimpleName());
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred", request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
