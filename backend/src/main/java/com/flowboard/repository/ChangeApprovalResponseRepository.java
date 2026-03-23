package com.flowboard.repository;

import com.flowboard.entity.ChangeApprovalResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeApprovalResponseRepository extends JpaRepository<ChangeApprovalResponse, UUID> {
    List<ChangeApprovalResponse> findByApprovalRequestId(UUID approvalRequestId);
    Optional<ChangeApprovalResponse> findByApprovalRequestIdAndUserId(UUID approvalRequestId, UUID userId);
    long countByApprovalRequestIdAndDecision(UUID approvalRequestId, ChangeApprovalResponse.ApprovalDecision decision);
}
