package com.aivideo.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {

    /** 可选：同时作废 refresh token */
    private String refreshToken;
}
