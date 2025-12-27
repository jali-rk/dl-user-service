package com.dopamine.userservice.service;

import com.dopamine.userservice.domain.EmailResetToken;
import com.dopamine.userservice.domain.User;
import com.dopamine.userservice.dto.EmailResetConfirmRequest;
import com.dopamine.userservice.dto.EmailResetRequest;
import com.dopamine.userservice.dto.EmailResetResponse;
import com.dopamine.userservice.exception.EmailAlreadyInUseException;
import com.dopamine.userservice.exception.InvalidEmailResetTokenException;
import com.dopamine.userservice.exception.UserNotFoundException;
import com.dopamine.userservice.repository.EmailResetTokenRepository;
import com.dopamine.userservice.repository.UserRepository;
import com.dopamine.userservice.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email Reset Flow - Service Unit Tests")
class EmailResetFlowServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailResetTokenRepository emailResetTokenRepository;

    // Unused by these tests but required by constructor
    @Mock private com.dopamine.userservice.repository.VerificationCodeRepository verificationCodeRepository;
    @Mock private com.dopamine.userservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private com.dopamine.userservice.mapper.UserMapper userMapper;
    @Mock private com.dopamine.userservice.service.StudentCodeGeneratorService studentCodeGeneratorService;
    @Mock private com.dopamine.userservice.service.EmailNotificationService emailNotificationService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private com.dopamine.userservice.service.impl.UserServiceImpl userService;

    @BeforeEach
    void setup() {
        // InjectMocks will instantiate UserServiceImpl, but passwordEncoder isn't a mock
        // so we re-create with explicit constructor to ensure it's wired.
        userService = new com.dopamine.userservice.service.impl.UserServiceImpl(
                userRepository,
                verificationCodeRepository,
                passwordResetTokenRepository,
                emailResetTokenRepository,
                userMapper,
                passwordEncoder,
                studentCodeGeneratorService,
                emailNotificationService
        );
    }

    @Test
    @DisplayName("requestEmailReset should create a token and return raw token")
    void requestEmailResetShouldCreateToken() {
        UUID userId = UUID.randomUUID();
        User user = TestDataBuilder.verifiedStudent().id(userId).email("old@example.com").build();

        when(userRepository.findByIdAndNotDeleted(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCaseAndNotDeleted("new@example.com")).thenReturn(Optional.empty());
        when(emailResetTokenRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());

        EmailResetRequest req = EmailResetRequest.builder()
                .userId(userId)
                .oldEmail("old@example.com")
                .newEmail("new@example.com")
                .build();

        EmailResetResponse res = userService.requestEmailReset(req);

        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getToken()).contains(".");

        ArgumentCaptor<EmailResetToken> tokenCaptor = ArgumentCaptor.forClass(EmailResetToken.class);
        verify(emailResetTokenRepository).save(tokenCaptor.capture());
        EmailResetToken saved = tokenCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getOldEmail()).isEqualTo("old@example.com");
        assertThat(saved.getNewEmail()).isEqualTo("new@example.com");
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getTokenId()).isNotNull();
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());

        // Ensure returned secret matches stored hash
        String secret = EmailResetFlowTestData.secret(res.getToken());
        assertThat(passwordEncoder.matches(secret, saved.getTokenHash())).isTrue();
    }

    @Test
    @DisplayName("requestEmailReset should 404 when oldEmail mismatches")
    void requestEmailResetShould404WhenOldEmailMismatch() {
        UUID userId = UUID.randomUUID();
        User user = TestDataBuilder.verifiedStudent().id(userId).email("real-old@example.com").build();

        when(userRepository.findByIdAndNotDeleted(userId)).thenReturn(Optional.of(user));

        EmailResetRequest req = EmailResetRequest.builder()
                .userId(userId)
                .oldEmail("wrong-old@example.com")
                .newEmail("new@example.com")
                .build();

        assertThatThrownBy(() -> userService.requestEmailReset(req))
                .isInstanceOf(UserNotFoundException.class);

        verify(emailResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestEmailReset should 409 when new email is used by another user")
    void requestEmailResetShould409WhenNewEmailUsed() {
        UUID userId = UUID.randomUUID();
        User user = TestDataBuilder.verifiedStudent().id(userId).email("old@example.com").build();
        User other = TestDataBuilder.verifiedStudent().id(UUID.randomUUID()).email("new@example.com").build();

        when(userRepository.findByIdAndNotDeleted(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCaseAndNotDeleted("new@example.com")).thenReturn(Optional.of(other));

        EmailResetRequest req = EmailResetRequest.builder()
                .userId(userId)
                .oldEmail("old@example.com")
                .newEmail("new@example.com")
                .build();

        assertThatThrownBy(() -> userService.requestEmailReset(req))
                .isInstanceOf(EmailAlreadyInUseException.class);

        verify(emailResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmEmailReset should update user email and mark token used")
    void confirmEmailResetShouldUpdateEmail() {
        UUID userId = UUID.randomUUID();
        User user = TestDataBuilder.verifiedStudent().id(userId).email("old@example.com").build();

        String secret = "test-secret";
        UUID tokenId = UUID.randomUUID();
        EmailResetToken tokenEntity = EmailResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .oldEmail("old@example.com")
                .newEmail("new@example.com")
                .tokenId(tokenId)
                .tokenHash(passwordEncoder.encode(secret))
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();

        when(emailResetTokenRepository.findValidByTokenId(eq(tokenId), any())).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findByEmailIgnoreCaseAndNotDeleted("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByIdAndNotDeleted(userId)).thenReturn(Optional.of(user));

        String combined = tokenId + "." + secret;

        var resp = userService.confirmEmailReset(EmailResetConfirmRequest.builder().token(combined).build());

        assertThat(resp.getNewEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(argThat(u -> u.getEmail().equals("new@example.com")));
        verify(emailResetTokenRepository, atLeastOnce()).save(argThat(t -> t.isUsed()));
    }

    @Test
    @DisplayName("confirmEmailReset should 400 for bad token format")
    void confirmEmailResetShould400ForBadFormat() {
        assertThatThrownBy(() -> userService.confirmEmailReset(EmailResetConfirmRequest.builder().token("bad").build()))
                .isInstanceOf(InvalidEmailResetTokenException.class);
    }

    @Test
    @DisplayName("confirmEmailReset should 409 when new email becomes used")
    void confirmEmailResetShould409WhenEmailAlreadyUsed() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        String secret = "test-secret";
        UUID tokenId = UUID.randomUUID();
        EmailResetToken tokenEntity = EmailResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .oldEmail("old@example.com")
                .newEmail("new@example.com")
                .tokenId(tokenId)
                .tokenHash(passwordEncoder.encode(secret))
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();

        when(emailResetTokenRepository.findValidByTokenId(eq(tokenId), any())).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findByEmailIgnoreCaseAndNotDeleted("new@example.com"))
                .thenReturn(Optional.of(TestDataBuilder.verifiedStudent().id(otherId).email("new@example.com").build()));

        assertThatThrownBy(() -> userService.confirmEmailReset(EmailResetConfirmRequest.builder().token(tokenId + "." + secret).build()))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }
}

