package com.flowboard.controller;

import com.flowboard.dto.CardDTO;
import com.flowboard.dto.StageDTO;
import com.flowboard.service.JWTService;
import com.flowboard.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class BoardController {
    private final ProjectService projectService;
    private final JWTService jwtService;

    @PostMapping("/{boardId}/stages")
    public ResponseEntity<StageDTO> addStage(
        @PathVariable UUID boardId,
        @RequestBody Map<String, String> request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(projectService.addStage(
            boardId,
            request.get("title"),
            request.get("color"),
            userId
        ));
    }

    @DeleteMapping("/stages/{stageId}")
    public ResponseEntity<Void> deleteStage(
        @PathVariable UUID stageId,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        projectService.deleteStage(stageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/stages/{stageId}")
    public ResponseEntity<StageDTO> renameStage(
        @PathVariable UUID stageId,
        @RequestBody Map<String, String> request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.renameStage(
            stageId,
            request.get("title"),
            userId
        ));
    }

    @PostMapping("/stages/{stageId}/cards")
    public ResponseEntity<CardDTO> createCard(
        @PathVariable UUID stageId,
        @RequestBody Map<String, String> request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.createCard(
            stageId,
            request.get("title"),
            request.get("description"),
            request.get("priority"),
            userId
        ));
    }

    @PutMapping("/cards/{cardId}")
    public ResponseEntity<CardDTO> updateCard(
        @PathVariable UUID cardId,
        @RequestBody Map<String, String> request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.updateCard(
            cardId,
            request.get("title"),
            request.get("description"),
            request.get("priority"),
            userId
        ));
    }

    @PutMapping("/cards/{cardId}/move")
    public ResponseEntity<CardDTO> moveCard(
        @PathVariable UUID cardId,
        @RequestBody Map<String, String> request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        
        UUID targetStageId = UUID.fromString(request.get("target_stage_id"));
        return ResponseEntity.ok(projectService.moveCard(cardId, targetStageId, userId));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(
        @PathVariable UUID cardId,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        projectService.deleteCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }
}
