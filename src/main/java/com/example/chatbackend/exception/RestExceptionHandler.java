package com.example.chatbackend.exception;

import com.example.chatbackend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "ValidationError", "Request validation failed", details, request);
    }

    @ExceptionHandler(ChatNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ChatNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NotFound", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ErrorResponse> handleRouteNotFound(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NotFound", "API route not found", Map.of("hint", "Check the request URL for spaces or newline characters."), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BadRequest", exception.getMessage(), Map.of("parameter", exception.getParameterName()), request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException exception, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "RateLimitExceeded", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ProviderTimeoutException.class)
    ResponseEntity<ErrorResponse> handleProviderTimeout(ProviderTimeoutException exception, HttpServletRequest request) {
        return error(HttpStatus.GATEWAY_TIMEOUT, "ProviderTimeout", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ProviderRateLimitException.class)
    ResponseEntity<ErrorResponse> handleProviderRateLimit(ProviderRateLimitException exception, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "ProviderRateLimited", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ProviderException.class)
    ResponseEntity<ErrorResponse> handleProvider(ProviderException exception, HttpServletRequest request) {
        Map<String, Object> details = Map.of("providerStatusCode", exception.getProviderStatusCode());
        return error(HttpStatus.BAD_GATEWAY, "ProviderError", exception.getMessage(), details, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BadRequest", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "InternalServerError", "Unexpected server error", Map.of(), request);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(code, message, details, Instant.now(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
