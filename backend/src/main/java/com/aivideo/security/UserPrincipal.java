package com.aivideo.security;

/**
 * 安全上下文中的当前登录用户。
 */
public record UserPrincipal(Long id, String username) {
}
