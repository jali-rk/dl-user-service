package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.CodePillarTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CodePillarTracker entity.
 */
@Repository
public interface CodePillarTrackerRepository extends JpaRepository<CodePillarTracker, UUID> {

    /**
     * Find tracker by sub-pillar base with pessimistic write lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CodePillarTracker c WHERE c.subPillarBase = :subPillarBase")
    Optional<CodePillarTracker> findBySubPillarBaseWithLock(@Param("subPillarBase") Integer subPillarBase);

    /**
     * Find tracker by sub-pillar base without lock.
     */
    Optional<CodePillarTracker> findBySubPillarBase(Integer subPillarBase);
}

