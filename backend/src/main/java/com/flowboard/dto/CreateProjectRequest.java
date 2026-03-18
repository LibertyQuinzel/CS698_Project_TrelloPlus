package com.flowboard.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {
    private String name;
    private String description;
    private Boolean generateTasks;
}
