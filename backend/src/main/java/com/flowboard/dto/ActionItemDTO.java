package com.flowboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItemDTO {
    private UUID id;
    private UUID meetingId;
    private String description;
    private String sourceContext;
    private String priority;
    private String status;
    private String approvalStatus;
    private String assignedToName;
    private UUID assignedToId;
    private LocalDateTime createdAt;
}
