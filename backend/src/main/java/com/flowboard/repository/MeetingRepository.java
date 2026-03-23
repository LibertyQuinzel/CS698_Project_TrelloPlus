package com.flowboard.repository;

import com.flowboard.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    List<Meeting> findByProjectId(UUID projectId);
    List<Meeting> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<Meeting> findByCreatedById(UUID userId);
}
