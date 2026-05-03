package com.flowboard.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetSecurityQuestionsRequest {
    private String currentPassword;
    private String securityQuestion1;
    private String securityAnswer1;
    private String securityQuestion2;
    private String securityAnswer2;

    @Size(max = 255, message = "Custom security question must be 255 characters or fewer")
    private String customSecurityQuestion;

    private String customSecurityAnswer;
}
