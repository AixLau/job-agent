package com.jobagent.server.controller;

import com.jobagent.server.dto.PluginErrorResponse;
import com.jobagent.server.service.DuplicateResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice(assignableTypes = PluginGatewayController.class)
public class PluginErrorHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<PluginErrorResponse> missingHeader(MissingRequestHeaderException ex) {
        PluginErrorResponse body = new PluginErrorResponse(
            "PLUGIN_TOKEN_INVALID",
            "missing plugin token",
            null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(DuplicateResponseException.class)
    public ResponseEntity<PluginErrorResponse> duplicate(DuplicateResponseException ex) {
        PluginErrorResponse body = new PluginErrorResponse(
            "DUPLICATE_IGNORED",
            "duplicate ignored",
            ex.getPayload()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<PluginErrorResponse> status(ResponseStatusException ex) {
        String code = ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason();
        PluginErrorResponse body = new PluginErrorResponse(code, code, null);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<PluginErrorResponse> validation(Exception ex) {
        PluginErrorResponse body = new PluginErrorResponse(
            "VALIDATION_FAILED",
            "validation failed",
            null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
