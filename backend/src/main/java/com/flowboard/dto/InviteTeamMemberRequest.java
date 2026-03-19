package com.flowboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteTeamMemberRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 254, message = "Email must be 254 characters or fewer")
    private String email;

    @Size(max = 100, message = "Full name must be 100 characters or fewer")
    private String fullName;

    @Pattern(regexp = "^(?i)(owner|editor|viewer)$", message = "Role must be owner, editor, or viewer")
    private String role;
}
