package com.flowboard.repository;

import com.flowboard.entity.ApprovalRequestSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRequestSummaryRepository extends JpaRepository<ApprovalRequestSummary, UUID> {
    Optional<ApprovalRequestSummary> findByMeetingId(UUID meetingId);
}
