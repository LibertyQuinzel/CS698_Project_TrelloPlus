package com.flowboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingSummaryDTO {
    private UUID id;
    private UUID meetingId;
    private String status;
    private String aiGeneratedContent;
    private LocalDateTime generatedAt;
    private LocalDateTime approvedAt;
    private List<ActionItemDTO> actionItems;
    private List<DecisionDTO> decisions;
    private List<ChangeDTO> changes;
}
