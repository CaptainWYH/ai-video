package com.aivideo.video.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVideoTaskRequest {

    @NotBlank(message = "prompt 不能为空")
    @Size(max = 2000, message = "prompt 不能超过 2000 个字符")
    private String prompt;

    /** 生成参数：风格、画幅、分辨率、时长和声音 */
    @Valid
    private VideoGenerationParams params;

    /** 可选：关联的视频创作项目 */
    private Long projectId;

    /** 幂等键：同一次点击或同一条聊天消息只创建一个视频任务 */
    @Size(max = 100, message = "clientRequestId 不能超过 100 个字符")
    private String clientRequestId;
}
