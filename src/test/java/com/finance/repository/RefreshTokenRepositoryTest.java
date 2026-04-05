package com.finance.repository;

import com.finance.entity.RefreshToken;
import com.finance.entity.User;
import com.finance.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository Tests")
class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .email("test@test.com").fullName("Test User")
                .password("encoded").role(Role.ANALYST).active(true).build());
    }

    private RefreshToken saveToken(boolean revoked, LocalDateTime expiresAt) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .revoked(revoked)
                .expiresAt(expiresAt)
                .build());
    }

    @Test
    @DisplayName("findByToken: returns token when it exists")
    void findByToken_exists() {
        RefreshToken saved = saveToken(false, LocalDateTime.now().plusDays(7));
        Optional<RefreshToken> found = refreshTokenRepository.findByToken(saved.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("findByToken: returns empty when token not found")
    void findByToken_notFound() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("revokeAllUserTokens: sets all user tokens to revoked")
    void revokeAllUserTokens_revokesAll() {
        saveToken(false, LocalDateTime.now().plusDays(7));
        saveToken(false, LocalDateTime.now().plusDays(3));

        refreshTokenRepository.revokeAllUserTokens(user);

        refreshTokenRepository.findAll().forEach(t ->
                assertThat(t.isRevoked()).isTrue());
    }

    @Test
    @DisplayName("isValid: false when token is revoked")
    void isValid_revokedToken_returnsFalse() {
        RefreshToken token = saveToken(true, LocalDateTime.now().plusDays(7));
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid: false when token is expired")
    void isValid_expiredToken_returnsFalse() {
        RefreshToken token = saveToken(false, LocalDateTime.now().minusHours(1));
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid: true when token is active and not expired")
    void isValid_activeToken_returnsTrue() {
        RefreshToken token = saveToken(false, LocalDateTime.now().plusDays(7));
        assertThat(token.isValid()).isTrue();
    }
}
