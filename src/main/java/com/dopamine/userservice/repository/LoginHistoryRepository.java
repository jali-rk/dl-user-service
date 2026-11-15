package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LoginHistory entity.
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    List<LoginHistory> findByUserIdOrderByLoggedInAtDesc(UUID userId);
}
