package com.flowboard.controller;

import com.flowboard.dto.GenerateSummaryRequest;
import com.flowboard.dto.MeetingSummaryDTO;
import com.flowboard.service.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/summaries")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SummaryController {
    private final SummaryService summaryService;

    /**
     * Generate summary from meeting transcript
     * POST /api/v1/summaries
     */
    @PostMapping
    public ResponseEntity<MeetingSummaryDTO> generateSummary(
        @Valid @RequestBody GenerateSummaryRequest request
    ) {
        MeetingSummaryDTO summary = summaryService.generateSummary(request.getMeetingId());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    /**
     * Get a summary by ID
     * GET /api/v1/summaries/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingSummaryDTO> getSummary(@PathVariable UUID id) {
        MeetingSummaryDTO summary = summaryService.getSummary(id);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get summary for a meeting
     * GET /api/v1/meetings/{meetingId}/summary
     */
    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<MeetingSummaryDTO> getSummaryByMeeting(@PathVariable UUID meetingId) {
        MeetingSummaryDTO summary = summaryService.getSummaryByMeeting(meetingId);
        return ResponseEntity.ok(summary);
    }
}
