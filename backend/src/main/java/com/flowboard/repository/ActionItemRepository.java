package com.flowboard.repository;

import com.flowboard.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, UUID> {
    List<ActionItem> findByMeetingId(UUID meetingId);
    List<ActionItem> findByAssignedToId(UUID userId);
}
