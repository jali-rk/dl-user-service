package com.dopamine.userservice.repository;

import com.dopamine.userservice.domain.Role;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.domain.UserStatus;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for UserRepository.
 * Tests database queries and custom repository methods.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("User Repository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve user")
    void shouldSaveAndRetrieveUser() {
        // Given
        User user = TestDataBuilder.defaultStudent().build();

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<User> retrieved = userRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Should find user by email case-insensitively")
    void shouldFindByEmailCaseInsensitive() {
        // Given
        User user = TestDataBuilder.defaultStudent()
                .email("test@example.com")
                .build();
        userRepository.save(user);

        // When
        Optional<User> found1 = userRepository.findByEmailIgnoreCaseAndNotDeleted("test@example.com");
        Optional<User> found2 = userRepository.findByEmailIgnoreCaseAndNotDeleted("TEST@EXAMPLE.COM");
        Optional<User> found3 = userRepository.findByEmailIgnoreCaseAndNotDeleted("TeSt@ExAmPlE.CoM");

        // Then
        assertThat(found1).isPresent();
        assertThat(found2).isPresent();
        assertThat(found3).isPresent();
        assertThat(found1.get().getId()).isEqualTo(found2.get().getId());
        assertThat(found2.get().getId()).isEqualTo(found3.get().getId());
    }

    @Test
    @DisplayName("Should not find soft-deleted users")
    void shouldNotFindSoftDeletedUsers() {
        // Given
        User user = TestDataBuilder.defaultStudent()
                .email("deleted@example.com")
                .deletedAt(Instant.now())
                .build();
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmailIgnoreCaseAndNotDeleted("deleted@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by code number")
    void shouldFindByCodeNumber() {
        // Given
        User user = TestDataBuilder.defaultStudent()
                .codeNumber("1001")
                .build();
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByCodeNumberAndNotDeleted("1001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCodeNumber()).isEqualTo("1001");
    }

    @Test
    @DisplayName("Should check email existence case-insensitively")
    void shouldCheckEmailExistence() {
        // Given
        User user = TestDataBuilder.defaultStudent()
                .email("exists@example.com")
                .build();
        userRepository.save(user);

        // When
        boolean exists1 = userRepository.existsByEmailIgnoreCaseAndNotDeleted("exists@example.com");
        boolean exists2 = userRepository.existsByEmailIgnoreCaseAndNotDeleted("EXISTS@EXAMPLE.COM");
        boolean notExists = userRepository.existsByEmailIgnoreCaseAndNotDeleted("notexists@example.com");

        // Then
        assertThat(exists1).isTrue();
        assertThat(exists2).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check code number existence")
    void shouldCheckCodeNumberExistence() {
        // Given
        User user = TestDataBuilder.defaultStudent()
                .codeNumber("1001")
                .build();
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByCodeNumberAndNotDeleted("1001");
        boolean notExists = userRepository.existsByCodeNumberAndNotDeleted("9999");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should get next student code number from sequence")
    void shouldGetNextStudentCodeNumber() {
        // When
        Long code1 = userRepository.getNextStudentCodeNumber();
        Long code2 = userRepository.getNextStudentCodeNumber();
        Long code3 = userRepository.getNextStudentCodeNumber();

        // Then
        assertThat(code1).isGreaterThanOrEqualTo(1001L);
        assertThat(code2).isEqualTo(code1 + 1);
        assertThat(code3).isEqualTo(code2 + 1);
    }

    @Test
    @DisplayName("Should find users by role")
    void shouldFindByRole() {
        // Given
        User student = TestDataBuilder.defaultStudent().email("student@example.com").build();
        User admin = TestDataBuilder.defaultAdmin().email("admin@example.com").build();
        User mainAdmin = TestDataBuilder.defaultMainAdmin().email("mainadmin@example.com").build();

        userRepository.saveAll(List.of(student, admin, mainAdmin));

        // When
        List<User> students = userRepository.findByRoleAndNotDeleted(Role.STUDENT);
        List<User> admins = userRepository.findByRoleAndNotDeleted(Role.ADMIN);
        List<User> mainAdmins = userRepository.findByRoleAndNotDeleted(Role.MAIN_ADMIN);

        // Then
        assertThat(students).hasSize(1);
        assertThat(students.get(0).getRole()).isEqualTo(Role.STUDENT);

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getRole()).isEqualTo(Role.ADMIN);

        assertThat(mainAdmins).hasSize(1);
        assertThat(mainAdmins.get(0).getRole()).isEqualTo(Role.MAIN_ADMIN);
    }

    @Test
    @DisplayName("Should find users by role and status")
    void shouldFindByRoleAndStatus() {
        // Given
        User activeAdmin = TestDataBuilder.defaultAdmin()
                .email("active@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        User inactiveAdmin = TestDataBuilder.defaultAdmin()
                .email("inactive@example.com")
                .status(UserStatus.INACTIVE)
                .build();

        userRepository.saveAll(List.of(activeAdmin, inactiveAdmin));

        // When
        List<User> activeAdmins = userRepository.findByRoleAndStatusAndNotDeleted(Role.ADMIN, UserStatus.ACTIVE);
        List<User> inactiveAdmins = userRepository.findByRoleAndStatusAndNotDeleted(Role.ADMIN, UserStatus.INACTIVE);

        // Then
        assertThat(activeAdmins).hasSize(1);
        assertThat(activeAdmins.get(0).getStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(inactiveAdmins).hasSize(1);
        assertThat(inactiveAdmins.get(0).getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmail() {
        // Given
        User user1 = TestDataBuilder.defaultStudent()
                .email("unique@example.com")
                .build();
        userRepository.save(user1);

        User user2 = TestDataBuilder.defaultStudent()
                .email("unique@example.com") // Same email
                .codeNumber("1002")
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            userRepository.save(user2);
            userRepository.flush();
        }).isInstanceOf(Exception.class); // Should throw constraint violation
    }

    @Test
    @DisplayName("Should enforce unique code number constraint")
    void shouldEnforceUniqueCodeNumber() {
        // Given
        User user1 = TestDataBuilder.defaultStudent()
                .email("user1@example.com")
                .codeNumber("1001")
                .build();
        userRepository.save(user1);

        User user2 = TestDataBuilder.defaultStudent()
                .email("user2@example.com")
                .codeNumber("1001") // Same code number
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            userRepository.save(user2);
            userRepository.flush();
        }).isInstanceOf(Exception.class); // Should throw constraint violation
    }

    @Test
    @DisplayName("Should allow null code numbers for multiple admins")
    void shouldAllowNullCodeNumbersForAdmins() {
        // Given
        User admin1 = TestDataBuilder.defaultAdmin()
                .email("admin1@example.com")
                .codeNumber(null)
                .build();
        User admin2 = TestDataBuilder.defaultAdmin()
                .email("admin2@example.com")
                .codeNumber(null)
                .build();

        // When/Then - Should not throw exception
        userRepository.saveAll(List.of(admin1, admin2));

        List<User> admins = userRepository.findByRoleAndNotDeleted(Role.ADMIN);
        assertThat(admins).hasSize(2);
    }
}

