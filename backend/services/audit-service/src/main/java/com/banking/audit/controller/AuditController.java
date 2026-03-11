package com.banking.audit.controller;

import com.banking.audit.entity.AuditLog;
import com.banking.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAll(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAll(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLog>> getByUser(@PathVariable String userId,
                                                     @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getByUserId(userId, pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLog>> getByEntity(@PathVariable String entityType,
                                                       @PathVariable String entityId,
                                                       @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getByEntity(entityType, entityId, pageable));
    }
}
