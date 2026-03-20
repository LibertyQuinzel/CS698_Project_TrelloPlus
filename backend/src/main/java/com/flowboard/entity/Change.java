package com.flowboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Change {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String afterState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeStatus status = ChangeStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by")
    private User appliedBy;

    private LocalDateTime appliedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ChangeType {
        MOVE_CARD, UPDATE_CARD, CREATE_CARD, DELETE_CARD
    }

    public enum ChangeStatus {
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        READY_FOR_APPLICATION,
        APPLYING,
        APPLIED,
        ROLLED_BACK,
        ARCHIVED
    }
}
