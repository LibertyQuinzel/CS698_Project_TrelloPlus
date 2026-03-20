package com.flowboard.repository;

import com.flowboard.entity.ApprovalResponseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalResponseSummaryRepository extends JpaRepository<ApprovalResponseSummary, UUID> {
    List<ApprovalResponseSummary> findByApprovalRequestId(UUID approvalRequestId);
    Optional<ApprovalResponseSummary> findByApprovalRequestIdAndUserId(UUID approvalRequestId, UUID userId);
    long countByApprovalRequestIdAndResponse(UUID approvalRequestId, ApprovalResponseSummary.ApprovalResponse response);
}
