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
public class ChangeHistoryEntryDTO {
    private UUID id;
    private String action;
    private UUID actorId;
    private String actorName;
    private String details;
    private LocalDateTime createdAt;
}
