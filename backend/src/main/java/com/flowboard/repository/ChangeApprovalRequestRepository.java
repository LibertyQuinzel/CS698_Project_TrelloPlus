package com.flowboard.repository;

import com.flowboard.entity.ChangeApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeApprovalRequestRepository extends JpaRepository<ChangeApprovalRequest, UUID> {
    Optional<ChangeApprovalRequest> findByChangeId(UUID changeId);
}
