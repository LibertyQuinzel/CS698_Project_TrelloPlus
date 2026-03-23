package com.flowboard.repository;

import com.flowboard.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StageRepository extends JpaRepository<Stage, UUID> {
    List<Stage> findByBoardIdOrderByPosition(UUID boardId);
}
