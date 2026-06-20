package com.toolshare.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "help_responses")
public class HelpResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "help_post_id", nullable = false)
    private Long helpPostId;

    @Column(name = "responder_id", nullable = false)
    private Long responderId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "contact_info", length = 200)
    private String contactInfo;

    @Column(name = "is_accepted", nullable = false)
    private boolean isAccepted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
