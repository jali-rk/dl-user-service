package com.dopamine.userservice.repository;
import com.dopamine.userservice.domain.PaperCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
/**
 * Repository for PaperCenter entity.
 */
@Repository
public interface PaperCenterRepository extends JpaRepository<PaperCenter, UUID> {
    /**
     * Find all paper centers ordered by name ascending.
     */
    List<PaperCenter> findAllByOrderByNameAsc();
    /**
     * Check if a paper center exists by name (case-sensitive).
     */
    boolean existsByName(String name);
}
