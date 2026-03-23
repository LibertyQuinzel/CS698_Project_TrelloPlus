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
public class ApprovalStatusDTO {
    private UUID meetingId;
    private Integer requiredApprovals;
    private Integer currentApprovedCount;
    private Integer currentRejectedCount;
    private Integer totalApproversNeeded;
    private List<ApprovalResponseDTO> responses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApprovalResponseDTO {
        private UUID userId;
        private String userName;
        private String response;
        private String comments;
        private LocalDateTime respondedAt;
    }
}
