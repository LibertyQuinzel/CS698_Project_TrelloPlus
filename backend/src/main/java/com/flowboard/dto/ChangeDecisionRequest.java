package com.flowboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeDecisionRequest {
    @NotBlank(message = "Decision is required")
    private String decision; // APPROVE | REJECT | DEFER

    private String feedback;
}
