package com.finance.service;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RefreshRequest;
import com.finance.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshRequest request);
    void logout(RefreshRequest request);
}
