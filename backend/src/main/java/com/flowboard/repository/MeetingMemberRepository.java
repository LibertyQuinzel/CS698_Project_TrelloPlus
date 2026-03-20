package com.flowboard.repository;

import com.flowboard.entity.MeetingMember;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
