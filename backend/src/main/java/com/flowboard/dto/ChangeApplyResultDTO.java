package com.flowboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeApplyResultDTO {
    private UUID changeId;
    private String status;
    private boolean applied;
    private String message;
}
