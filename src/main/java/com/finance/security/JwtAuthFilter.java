package com.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String path = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        log.debug("JWT Filter — {} {} | Auth header present: {}",
                request.getMethod(), path, authHeader != null);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("JWT Filter — No Bearer token, passing through for: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Clean token — strip spaces and accidental quote characters
        String jwt = authHeader.substring(7).trim()
                .replace("\"", "")
                .replace("'", "");

        log.debug("JWT Filter — Token extracted, length: {}", jwt.length());

        if (jwt.isEmpty()) {
            log.warn("JWT Filter — Empty token after Bearer prefix for: {}", path);
            sendUnauthorized(response, "Token is empty. Please login again.");
            return;
        }

        try {
            final String userEmail = jwtUtils.extractUsername(jwt);
            log.debug("JWT Filter — Token subject (email): {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.debug("JWT Filter — Loaded user: {} | Authorities: {}",
                        userDetails.getUsername(), userDetails.getAuthorities());

                if (jwtUtils.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT Filter — Authentication set successfully for: {} with roles: {}",
                            userEmail, userDetails.getAuthorities());
                } else {
                    log.warn("JWT Filter — Token INVALID or EXPIRED for user: {}", userEmail);
                    sendUnauthorized(response, "Token is invalid or expired. Please login again.");
                    return;
                }
            } else {
                log.debug("JWT Filter — Already authenticated or null email, skipping");
            }

        } catch (Exception e) {
            log.warn("JWT Filter — Token parsing FAILED: {}", e.getMessage());
            sendUnauthorized(response, "Invalid token. Please login again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"[UNAUTHORIZED] " + message + "\"}"
        );
    }
}
