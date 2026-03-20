package com.flowboard.repository;

import com.flowboard.entity.ChangeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeSnapshotRepository extends JpaRepository<ChangeSnapshot, UUID> {
    List<ChangeSnapshot> findByChangeIdOrderByCreatedAtDesc(UUID changeId);
}
