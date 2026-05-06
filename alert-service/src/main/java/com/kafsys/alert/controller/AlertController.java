package com.kafsys.alert.controller;

import com.kafsys.alert.dto.AlertResponse;
import com.kafsys.alert.service.AlertService;
import com.kafsys.common.dto.ApiResponse;
import com.kafsys.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Real-time banking alert history and notification management")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List alerts for an account")
    public ResponseEntity<ApiResponse<PagedResponse<AlertResponse>>> getAlerts(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AlertResponse> result = accountId != null
                ? alertService.getAlertsByAccount(accountId, unreadOnly, page, size)
                : alertService.getAllAlerts(page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<ApiResponse<AlertResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getById(id)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark alert as read")
    public ResponseEntity<ApiResponse<AlertResponse>> markRead(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.markAsRead(id), "Alert marked as read"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread alerts for an account")
    public ResponseEntity<ApiResponse<Long>> countUnread(@RequestParam String accountId) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.countUnread(accountId)));
    }
}
