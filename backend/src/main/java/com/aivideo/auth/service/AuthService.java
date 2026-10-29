package com.aivideo.auth.service;

import com.aivideo.auth.dto.LoginRequest;
import com.aivideo.auth.dto.RegisterRequest;
import com.aivideo.auth.dto.TokenResponse;
import com.aivideo.auth.dto.UserInfoResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    UserInfoResponse currentUser();
}
