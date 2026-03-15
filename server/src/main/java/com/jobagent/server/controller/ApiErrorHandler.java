package com.jobagent.server.controller;

import com.jobagent.server.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {
    AuthController.class,
    TaskController.class,
    ResumeController.class,
    DashboardController.class,
    JobActionController.class,
    AuditController.class,
    DraftController.class,
    ConversationController.class
})
public class ApiErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> status(ResponseStatusException ex) {
        String code = ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason();
        ApiErrorResponse body = new ApiErrorResponse(new ApiErrorResponse.ApiError(code, code));
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
