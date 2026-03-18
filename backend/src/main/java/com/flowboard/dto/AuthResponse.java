package com.flowboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    @JsonProperty("expires_in")
    private long expiresIn;
    private UserDTO user;
}
