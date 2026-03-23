package com.flowboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String username;
    private String fullName;
    private String role;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
