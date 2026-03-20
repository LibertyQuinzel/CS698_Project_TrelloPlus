package com.flowboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitApprovalRequest {
    @NotNull(message = "Meeting ID is required")
    private UUID meetingId;

    @NotBlank(message = "Response is required (APPROVED or REJECTED)")
    private String response;

    private String comments;
}
