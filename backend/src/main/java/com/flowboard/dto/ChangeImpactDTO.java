package com.flowboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeImpactDTO {
    private List<String> affectedCards;
    private List<String> affectedStages;
    private String riskLevel;
    private List<String> potentialConflicts;
}
