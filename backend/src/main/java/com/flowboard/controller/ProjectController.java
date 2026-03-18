package com.flowboard.controller;

import com.flowboard.dto.*;
import com.flowboard.entity.User;
import com.flowboard.repository.UserRepository;
import com.flowboard.service.JWTService;
import com.flowboard.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectController {
    private final ProjectService projectService;
    private final JWTService jwtService;
    private final UserRepository userRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<ProjectDTO> createProject(
        @RequestBody CreateProjectRequest request,
        @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(projectService.createProject(request, user));
    }

    @GetMapping
    @Transactional
    public ResponseEntity<List<ProjectDTO>> getUserProjects(
        @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        
        return ResponseEntity.ok(projectService.getUserProjects(userId));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(
        @PathVariable UUID projectId,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.getProject(projectId, userId));
    }

    @PutMapping("/{projectId}")
    @Transactional
    public ResponseEntity<ProjectDTO> updateProject(
        @PathVariable UUID projectId,
        @RequestBody UpdateProjectRequest request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.updateProject(projectId, userId, request));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
        @PathVariable UUID projectId,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        projectService.deleteProject(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<TeamMemberDTO>> getProjectMembers(
        @PathVariable UUID projectId,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.getProjectMembers(projectId, userId));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<TeamMemberDTO> addTeamMember(
        @PathVariable UUID projectId,
        @RequestBody InviteTeamMemberRequest request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.addTeamMember(projectId, request.getEmail(), request.getRole(), userId));
    }

    @PutMapping("/{projectId}/members/{userId}")
    public ResponseEntity<TeamMemberDTO> updateTeamMemberRole(
        @PathVariable UUID projectId,
        @PathVariable UUID userId,
        @RequestBody UpdateTeamMemberRoleRequest request,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID requesterId = jwtService.extractUserId(token);

        return ResponseEntity.ok(projectService.updateTeamMemberRole(projectId, userId, request.getRole(), requesterId));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeTeamMember(
        @PathVariable UUID projectId,
        @PathVariable UUID userId,
        @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        UUID requesterId = jwtService.extractUserId(token);

        projectService.removeTeamMember(projectId, userId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
