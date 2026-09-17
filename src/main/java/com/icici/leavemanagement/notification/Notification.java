package com.icici.leavemanagement.notification;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long recipientId;
    private String message;
    @Column(name = "is_read")   // READ is a reserved word in MySQL
    private boolean read;
    private LocalDateTime createdAt;

    public Notification(Long recipientId, String message) {
        this.recipientId = recipientId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
