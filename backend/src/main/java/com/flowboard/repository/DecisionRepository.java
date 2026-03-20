package com.flowboard.repository;

import com.flowboard.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {
    List<Decision> findByMeetingId(UUID meetingId);
}
