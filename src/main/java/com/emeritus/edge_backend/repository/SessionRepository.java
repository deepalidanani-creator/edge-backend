package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByTenantId(String tenantId);

    Optional<Session> findByIdAndTenantId(Long id, String tenantId);

    Optional<Session> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    // Count how many sessions a specific company has scheduled for a specific topic
    long countByTenantIdAndTopicId(String tenantId, Long topicId);

    // Count overall global scheduled masterclasses to prevent exceeding the Bucket tier limit
    long countByTenantId(String tenantId);

    @Query("SELECT s FROM Session s WHERE s.tenantId = :tenantId "
         + "AND s.sessionDate >= :today AND s.audienceAll = true "
         + "ORDER BY s.sessionDate ASC")
    List<Session> findUpcomingAudienceAllSessions(
        @Param("tenantId") String tenantId,
        @Param("today") LocalDate today);

    @Query("SELECT DISTINCT s FROM Session s JOIN s.cohortIds c "
         + "WHERE s.tenantId = :tenantId AND s.sessionDate >= :today "
         + "AND s.audienceAll = false AND c IN :cohortIds "
         + "ORDER BY s.sessionDate ASC")
    List<Session> findUpcomingCohortTargetedSessions(
        @Param("tenantId") String tenantId,
        @Param("today") LocalDate today,
        @Param("cohortIds") List<String> cohortIds);
}
