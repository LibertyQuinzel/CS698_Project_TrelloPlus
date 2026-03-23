package com.flowboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageDTO {
    private UUID id;
    private String title;
    private String color;
    private List<CardDTO> cards;
}
