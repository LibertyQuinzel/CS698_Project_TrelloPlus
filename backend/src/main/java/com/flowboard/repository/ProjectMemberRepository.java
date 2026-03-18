package com.flowboard.repository;

import com.flowboard.entity.Project;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends Repository<Project, UUID> {
    @Modifying
    @Query(value = "INSERT INTO project_members (project_id, user_id, role) VALUES (:projectId, :userId, :role) " +
        "ON CONFLICT (project_id, user_id) DO UPDATE SET role = :role", nativeQuery = true)
    void upsertMemberRole(@Param("projectId") UUID projectId, @Param("userId") UUID userId, @Param("role") String role);

    @Modifying
    @Query(value = "DELETE FROM project_members WHERE project_id = :projectId AND user_id = :userId", nativeQuery = true)
    void deleteMember(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Query(value = "SELECT role FROM project_members WHERE project_id = :projectId AND user_id = :userId", nativeQuery = true)
    Optional<String> findMemberRole(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Query(value = "SELECT user_id, role FROM project_members WHERE project_id = :projectId", nativeQuery = true)
    List<Object[]> findProjectMemberRoles(@Param("projectId") UUID projectId);
}
