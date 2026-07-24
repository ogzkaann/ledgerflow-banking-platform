package dev.oguzkaandere.ledgerflow.transfer.adapter.in.web;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.IdempotencyConflictException;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferTransitionException;
import dev.oguzkaandere.ledgerflow.transfer.domain.exception.TransferNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, "Request validation failed", "One or more request fields are invalid", request);
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("field", error.getField());
                    item.put("message", error.getDefaultMessage());
                    return item;
                })
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed(Exception exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "The request contains malformed JSON or identifiers",
                request);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, IllegalArgumentException.class})
    ProblemDetail invalidQuery(Exception exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                exception instanceof IllegalArgumentException
                        ? exception.getMessage()
                        : "One or more request parameters are invalid",
                request);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, InvalidHeaderException.class})
    ProblemDetail invalidHeader(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request header", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransferException.class)
    ProblemDetail invalidTransfer(InvalidTransferException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid transfer", exception.getMessage(), request);
    }

    @ExceptionHandler(TransferNotFoundException.class)
    ProblemDetail notFound(TransferNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Transfer not found", exception.getMessage(), request);
    }

    @ExceptionHandler({IdempotencyConflictException.class, InvalidTransferTransitionException.class})
    ProblemDetail conflict(RuntimeException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Transfer conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
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
