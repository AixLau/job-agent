package com.jobagent.server.controller;

import com.jobagent.server.dto.AuditItem;
import com.jobagent.server.dto.AuditListResponse;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.service.AuthService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
public class AuditController {

    private final AuthService authService;
    private final AuditLogRepository auditLogRepository;

    public AuditController(AuthService authService, AuditLogRepository auditLogRepository) {
        this.authService = authService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public AuditListResponse list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        String userId = authService.requireUserId(authorization);
        var auditPage = auditLogRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size));
        List<AuditItem> items = auditPage.getContent().stream()
            .map(entity -> new AuditItem(
                entity.getActionType(),
                entity.getCreatedAt(),
                null,
                entity.getPayload(),
                null,
                List.of()
            ))
            .toList();
        return new AuditListResponse(items, page, size, auditPage.getTotalElements());
    }
}
