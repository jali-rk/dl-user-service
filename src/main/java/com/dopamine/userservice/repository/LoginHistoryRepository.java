package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LoginHistory entity.
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    List<LoginHistory> findByUserIdOrderByLoggedInAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM LoginHistory lh WHERE lh.loggedInAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
