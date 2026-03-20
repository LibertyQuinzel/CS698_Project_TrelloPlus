package com.flowboard.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
    @NotBlank(message = "Meeting title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must be 1000 characters or fewer")
    private String description;

    @NotNull(message = "Meeting date is required")
    private LocalDate meetingDate;

    private LocalTime meetingTime;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @Size(max = 50, message = "Platform must be 50 characters or fewer")
    private String platform;

    @Size(max = 500, message = "Meeting link must be 500 characters or fewer")
    @Pattern(regexp = "^(https?://)?.*", message = "Meeting link should be a valid URL")
    private String meetingLink;

    private List<UUID> additionalMemberIds;
}
