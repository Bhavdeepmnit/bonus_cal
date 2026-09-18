package com.icici.leavemanagement.notification;

import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** The logged-in user's own notifications. */
@Tag(name = "6. Notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    // GET /api/notifications?unreadOnly=true&page=0&size=20  (newest first)
    @Operation(summary = "My notifications (newest first)")
    @GetMapping
    public PagedModel<Notification> getMine(
            @Parameter(example = "true") @RequestParam(defaultValue = "false") boolean unreadOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.getMine(unreadOnly, pageable));
    }

    // PUT /api/notifications/1/read
    @Operation(summary = "Mark one notification read")
    @PutMapping("/{id}/read")
    public Notification markRead(@Parameter(example = "1") @PathVariable Long id) { return service.markRead(id); }

    // PUT /api/notifications/read-all  -> {"updated": 3}
    @Operation(summary = "Mark all my notifications read")
    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead() { return Map.of("updated", service.markAllRead()); }
}
