package com.flowboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberDTO {
    private UUID id;
    private String email;
    private String username;
    private String fullName;
    private String role;
    private LocalDateTime createdAt;
}
