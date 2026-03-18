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
public class BoardDTO {
    private UUID id;
    private String name;
    @JsonProperty("project_id")
    private UUID projectId;
    private List<StageDTO> stages;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
