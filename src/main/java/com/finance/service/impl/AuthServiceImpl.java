package com.finance.service.impl;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RefreshRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.entity.RefreshToken;
import com.finance.entity.User;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.RefreshTokenRepository;
import com.finance.security.JwtUtils;
import com.finance.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) auth.getPrincipal();

        refreshTokenRepository.revokeAllUserTokens(user);

        String accessToken  = jwtUtils.generateAccessToken(user);
        String refreshValue = createAndSaveRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(accessToken, refreshValue, user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found or already used"));

        if (!stored.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked. Please log in again.");
        }

        // Rotate: revoke old, issue new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccess  = jwtUtils.generateAccessToken(user);
        String newRefresh = createAndSaveRefreshToken(user);

        log.info("Tokens rotated for user: {}", user.getEmail());
        return buildAuthResponse(newAccess, newRefresh, user);
    }

    @Override
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("User logged out, token revoked for: {}", token.getUser().getEmail());
                });
    }


    private String createAndSaveRefreshToken(User user) {
        String tokenValue = jwtUtils.generateRefreshTokenValue();
        long expiryMs     = jwtUtils.getRefreshTokenExpiryMs();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(expiryMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtils.getAccessTokenExpiryMs() / 1000)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
