package com.flowboard.repository;

import com.flowboard.entity.Change;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeRepository extends JpaRepository<Change, UUID> {
    List<Change> findByMeetingId(UUID meetingId);
    List<Change> findByStatus(Change.ChangeStatus status);
    List<Change> findByMeetingIdAndStatus(UUID meetingId, Change.ChangeStatus status);
    List<Change> findByMeetingProjectIdAndStatus(UUID projectId, Change.ChangeStatus status);
    List<Change> findByMeetingProjectId(UUID projectId);
}
