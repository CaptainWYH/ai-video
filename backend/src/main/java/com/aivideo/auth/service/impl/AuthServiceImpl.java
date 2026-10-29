package com.aivideo.auth.service.impl;

import com.aivideo.auth.dto.LoginRequest;
import com.aivideo.auth.dto.RegisterRequest;
import com.aivideo.auth.dto.TokenResponse;
import com.aivideo.auth.dto.UserInfoResponse;
import com.aivideo.auth.service.AuthService;
import com.aivideo.common.BizException;
import com.aivideo.security.JwtProperties;
import com.aivideo.security.JwtTokenProvider;
import com.aivideo.security.SecurityUtils;
import com.aivideo.security.TokenStore;
import com.aivideo.user.entity.User;
import com.aivideo.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            throw BizException.conflict("用户名已存在");
        }
        if (userService.existsByEmail(request.getEmail())) {
            throw BizException.conflict("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        userService.create(user);

        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return issueTokens(user);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userService.findByUsernameOrEmail(request.getAccount());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用");
        }
        return issueTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseQuietly(refreshToken);
        if (claims == null || !jwtTokenProvider.isType(claims, JwtTokenProvider.TYPE_REFRESH)) {
            throw BizException.unauthorized("refresh token 无效或已过期");
        }

        Long userId = jwtTokenProvider.getUserId(claims);
        Long storedUserId = tokenStore.getRefreshTokenUserId(claims.getId());
        if (storedUserId == null || !storedUserId.equals(userId)) {
            throw BizException.unauthorized("refresh token 已失效，请重新登录");
        }

        // 旋转 refresh token：旧 token 立即失效
        tokenStore.deleteRefreshToken(claims.getId());

        User user = userService.findById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.unauthorized("用户不存在或已被禁用");
        }
        return issueTokens(user);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            Claims claims = parseQuietly(accessToken);
            if (claims != null && jwtTokenProvider.isType(claims, JwtTokenProvider.TYPE_ACCESS)) {
                tokenStore.blacklistAccessToken(claims.getId(), jwtTokenProvider.remainingTtl(claims));
            }
        }
        if (StringUtils.hasText(refreshToken)) {
            Claims claims = parseQuietly(refreshToken);
            if (claims != null && jwtTokenProvider.isType(claims, JwtTokenProvider.TYPE_REFRESH)) {
                tokenStore.deleteRefreshToken(claims.getId());
            }
        }
    }

    @Override
    public UserInfoResponse currentUser() {
        User user = userService.findById(SecurityUtils.currentUserId());
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return UserInfoResponse.from(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        Claims refreshClaims = jwtTokenProvider.parse(refreshToken);
        tokenStore.saveRefreshToken(refreshClaims.getId(), user.getId(),
                jwtProperties.getRefreshTokenTtl());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenTtl().toSeconds())
                .user(UserInfoResponse.from(user))
                .build();
    }

    private Claims parseQuietly(String token) {
        try {
            return jwtTokenProvider.parse(token);
        } catch (Exception ex) {
            log.debug("token 解析失败: {}", ex.getMessage());
            return null;
        }
    }
}
