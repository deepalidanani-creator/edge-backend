package com.emeritus.edge_backend.repository;

import com.emeritus.edge_backend.entity.TenantBucket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TenantBucketRepository extends JpaRepository<TenantBucket, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TenantBucket b WHERE b.tenantId = :tenantId")
    Optional<TenantBucket> findByTenantIdForUpdate(@Param("tenantId") String tenantId);
}
