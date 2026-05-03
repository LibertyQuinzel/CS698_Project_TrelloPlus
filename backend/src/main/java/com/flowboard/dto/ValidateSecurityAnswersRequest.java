package com.flowboard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateSecurityAnswersRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @Valid
    @NotNull(message = "Answers are required")
    private SecurityAnswers answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityAnswers {
        @NotBlank(message = "Answer 1 is required")
        private String answer1;

        @NotBlank(message = "Answer 2 is required")
        private String answer2;

        @NotBlank(message = "Custom answer is required")
        private String customAnswer;
    }
}
