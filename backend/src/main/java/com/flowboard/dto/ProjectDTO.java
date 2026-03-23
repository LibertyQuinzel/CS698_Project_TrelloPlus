package com.flowboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {
    private UUID id;
    private String name;
    private String description;
    @JsonProperty("board_id")
    private UUID boardId;
    private List<UserDTO> members;
    private List<StageDTO> columns;
    private List<CardDTO> tasks;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
