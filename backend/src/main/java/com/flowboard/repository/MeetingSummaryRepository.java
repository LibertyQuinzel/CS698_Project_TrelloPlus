package com.flowboard.repository;

import com.flowboard.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, UUID> {
    Optional<MeetingSummary> findByMeetingId(UUID meetingId);
}
