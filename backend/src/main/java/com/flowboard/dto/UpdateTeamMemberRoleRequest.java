package com.flowboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTeamMemberRoleRequest {
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(?i)(editor|viewer)$", message = "Role must be editor or viewer")
    private String role;
}
