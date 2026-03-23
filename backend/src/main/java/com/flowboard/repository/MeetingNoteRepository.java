package com.flowboard.repository;

import com.flowboard.entity.MeetingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingNoteRepository extends JpaRepository<MeetingNote, UUID> {
    List<MeetingNote> findByMeetingId(UUID meetingId);
}
