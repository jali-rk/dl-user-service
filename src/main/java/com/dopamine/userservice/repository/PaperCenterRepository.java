package com.dopamine.userservice.repository;
import com.dopamine.userservice.domain.PaperCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;  
import org.springframework.data.jpa.repository.Query;     
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
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
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM PaperCenter pc WHERE pc.deletedAt IS NULL AND pc.name = :name")
    boolean existsActiveByName(@Param("name") String name);
    
    @Query("SELECT pc FROM PaperCenter pc WHERE pc.id = :id AND pc.deletedAt IS NULL")
    Optional<PaperCenter> findActiveById(@Param("id") UUID id);
    
    // @Query("SELECT pc FROM PaperCenter pc WHERE pc.id = :id")
    // Optional<PaperCenter> findByIdIncludingDeleted(@Param("id") UUID id);
    
    // @Query("SELECT pc FROM PaperCenter pc ORDER BY pc.name ASC")
    // List<PaperCenter> findAllIncludingDeleted();
    
    @Modifying
    @Transactional
    @Query("UPDATE PaperCenter pc SET pc.deletedAt = CURRENT_TIMESTAMP WHERE pc.id = :id")
    int softDeleteById(@Param("id") UUID id);
    
    @Modifying
    @Transactional
    @Query(value = "UPDATE paper_centers SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    int restoreById(@Param("id") UUID id);

    @Query(value = "SELECT * FROM paper_centers ORDER BY name ASC", nativeQuery = true)
    List<PaperCenter> findAllIncludingDeleted();

    @Query(value = "SELECT * FROM paper_centers WHERE id = :id", nativeQuery = true)
    Optional<PaperCenter> findByIdIncludingDeleted(@Param("id") UUID id);

}
