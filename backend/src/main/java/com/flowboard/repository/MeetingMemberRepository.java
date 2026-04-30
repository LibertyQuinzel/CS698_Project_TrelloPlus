package com.flowboard.repository;

import com.flowboard.entity.MeetingMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingMemberRepository extends JpaRepository<MeetingMember, UUID> {
    List<MeetingMember> findByMeetingId(UUID meetingId);
    boolean existsByMeetingIdAndUserId(UUID meetingId, UUID userId);
    Optional<MeetingMember> findByMeetingIdAndUserId(UUID meetingId, UUID userId);
    void deleteByMeetingIdAndUserId(UUID meetingId, UUID userId);
    long countByMeetingId(UUID meetingId);
    
    /**
     * Remove a user from all meetings in a project.
     * Called when a user is removed from a project.
     */
    @Modifying
    @Query(value = "DELETE FROM meeting_members WHERE user_id = :userId AND meeting_id IN " +
        "(SELECT id FROM meetings WHERE project_id = :projectId)", nativeQuery = true)
    void deleteUserFromProjectMeetings(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
