package com.flowboard.repository;

import com.flowboard.entity.ChangeAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeAuditEntryRepository extends JpaRepository<ChangeAuditEntry, UUID> {
    List<ChangeAuditEntry> findByChangeIdOrderByCreatedAtDesc(UUID changeId);
}
