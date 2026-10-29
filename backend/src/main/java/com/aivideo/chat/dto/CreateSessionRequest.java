package com.aivideo.chat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSessionRequest {

    @Size(max = 200, message = "会话标题不能超过 200 个字符")
    private String title;
}
