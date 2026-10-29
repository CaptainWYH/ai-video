package com.aivideo.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class CreateVideoTaskRequest {

    @NotBlank(message = "prompt 不能为空")
    @Size(max = 2000, message = "prompt 不能超过 2000 个字符")
    private String prompt;

    /** 生成参数：resolution、duration、style 等，原样透传 */
    private Map<String, Object> params;

    /** 可选：关联的视频创作项目 */
    private Long projectId;
}
