package com.icici.leavemanagement.notification;

import java.util.Map;

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

import lombok.RequiredArgsConstructor;

/** The logged-in user's own notifications. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    // GET /api/notifications?unreadOnly=true&page=0&size=20  (newest first)
    @GetMapping
    public PagedModel<Notification> getMine(@RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.getMine(unreadOnly, pageable));
    }

    // PUT /api/notifications/1/read
    @PutMapping("/{id}/read")
    public Notification markRead(@PathVariable Long id) { return service.markRead(id); }

    // PUT /api/notifications/read-all  -> {"updated": 3}
    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead() { return Map.of("updated", service.markAllRead()); }
}
