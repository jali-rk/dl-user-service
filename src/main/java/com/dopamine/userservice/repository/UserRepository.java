package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email (case-insensitive), excluding soft-deleted users.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<User> findByEmailIgnoreCaseAndNotDeleted(@Param("email") String email);

    /**
     * Find a user by code number, excluding soft-deleted users.
     */
    @Query("SELECT u FROM User u WHERE u.codeNumber = :codeNumber AND u.deletedAt IS NULL")
    Optional<User> findByCodeNumberAndNotDeleted(@Param("codeNumber") String codeNumber);

    /**
     * Find a user by ID, excluding soft-deleted users.
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Find all users by role, excluding soft-deleted users.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    List<User> findByRoleAndNotDeleted(@Param("role") Role role);

    /**
     * Find all users by role and status, excluding soft-deleted users.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status AND u.deletedAt IS NULL")
    List<User> findByRoleAndStatusAndNotDeleted(@Param("role") Role role, @Param("status") UserStatus status);

    /**
     * Find all active and verified students.
     * Returns students with role = STUDENT, status = ACTIVE, and isVerified = true.
     */
    @Query("SELECT u FROM User u WHERE u.role = com.dopamine.userservice.domain.Role.STUDENT " +
           "AND u.status = com.dopamine.userservice.domain.UserStatus.ACTIVE " +
           "AND u.isVerified = true " +
           "AND u.deletedAt IS NULL " +
           "ORDER BY u.createdAt DESC")
    List<User> findAllActiveVerifiedStudents();

    /**
     * Check if a user exists with the given email (case-insensitive), excluding soft-deleted users.
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    boolean existsByEmailIgnoreCaseAndNotDeleted(@Param("email") String email);

    /**
     * Check if a user exists with the given code number, excluding soft-deleted users.
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.codeNumber = :codeNumber AND u.deletedAt IS NULL")
    boolean existsByCodeNumberAndNotDeleted(@Param("codeNumber") String codeNumber);

    /**
     * Get the next value from the student code number sequence.
     */
    @Query(value = "SELECT nextval('student_code_number_seq')", nativeQuery = true)
    Long getNextStudentCodeNumber();

    /**
     * Find all users by IDs in bulk, excluding soft-deleted users.
     * Efficient for batch operations with up to 1000 IDs.
     *
     * IMPORTANT: Filters by status = ACTIVE and isVerified = true.
     * This is specifically for the batch public endpoint used by BFF.
     */
    @Query("SELECT u FROM User u WHERE u.id IN :ids " +
           "AND u.deletedAt IS NULL " +
           "AND u.status = com.dopamine.userservice.domain.UserStatus.ACTIVE " +
           "AND u.isVerified = true")
    List<User> findByIdsAndNotDeleted(@Param("ids") List<UUID> ids);
}


