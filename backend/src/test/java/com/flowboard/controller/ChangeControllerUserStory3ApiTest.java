package com.flowboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.dto.*;
import com.flowboard.entity.User;
import com.flowboard.repository.UserRepository;
import com.flowboard.service.ChangeApplicationService;
import com.flowboard.service.ChangeApprovalService;
import com.flowboard.service.ChangePreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChangeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChangeControllerUserStory3ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChangePreviewService changePreviewService;

    @MockBean
    private ChangeApprovalService changeApprovalService;

    @MockBean
    private ChangeApplicationService changeApplicationService;

    @MockBean
    private UserRepository userRepository;

    private UUID changeId;
    private UUID meetingId;

    @BeforeEach
    void setUp() {
        changeId = UUID.randomUUID();
        meetingId = UUID.randomUUID();

        User user = User.builder()
            .id(UUID.randomUUID())
            .email("reviewer@example.com")
            .username("reviewer")
            .role(User.UserRole.MEMBER)
            .build();

        when(userRepository.findByEmailIgnoreCase("reviewer@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void listChanges_returnsOk() throws Exception {
        when(changePreviewService.listChanges(null, null, null))
            .thenReturn(Collections.singletonList(ChangeDTO.builder()
                .id(changeId)
                .meetingId(meetingId)
                .changeType("UPDATE_CARD")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build()));

        mockMvc.perform(get("/changes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(changeId.toString()));
    }

    @Test
    void getChange_returnsOk() throws Exception {
        when(changePreviewService.getChange(changeId)).thenReturn(ChangeDTO.builder()
            .id(changeId)
            .meetingId(meetingId)
            .changeType("MOVE_CARD")
            .status("UNDER_REVIEW")
            .createdAt(LocalDateTime.now())
            .build());

        mockMvc.perform(get("/changes/{id}", changeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(changeId.toString()));
    }

    @Test
    void getDiff_returnsOk() throws Exception {
        when(changePreviewService.getDiff(changeId)).thenReturn(ChangeDiffDTO.builder()
            .beforeState("{}")
            .afterState("{}")
            .summary("Card fields were updated")
            .build());

        mockMvc.perform(get("/changes/{id}/diff", changeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    @WithMockUser(username = "reviewer@example.com")
    void approve_returnsApprovalStatus() throws Exception {
        ChangeDecisionRequest request = ChangeDecisionRequest.builder()
            .decision("APPROVE")
            .feedback("Looks good")
            .build();

        when(changeApprovalService.decide(eq(changeId), any(User.class), any(ChangeDecisionRequest.class)))
            .thenReturn(ApprovalStatusDTO.builder()
                .meetingId(meetingId)
                .requiredApprovals(2)
                .currentApprovedCount(1)
                .currentRejectedCount(0)
                .totalApproversNeeded(3)
                .responses(Collections.emptyList())
                .build());

        mockMvc.perform(post("/changes/{id}/approve", changeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiredApprovals").value(2));
    }

    @Test
    @WithMockUser(username = "reviewer@example.com")
    void apply_returnsResult() throws Exception {
        when(changeApplicationService.applyChange(eq(changeId), any(User.class)))
            .thenReturn(ChangeApplyResultDTO.builder()
                .changeId(changeId)
                .status("APPLIED")
                .applied(true)
                .message("Change applied successfully")
                .build());

        mockMvc.perform(post("/changes/{id}/apply", changeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPLIED"));
    }
}
