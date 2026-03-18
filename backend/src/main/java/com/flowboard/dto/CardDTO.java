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
public class CardDTO {
    private UUID id;
    private String title;
    private String description;
    private String priority;
    @JsonProperty("column_id")
    private UUID stageId;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    private UserDTO assignee;
}
