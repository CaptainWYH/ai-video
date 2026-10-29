package com.aivideo.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称不能超过 200 个字符")
    private String name;

    @Size(max = 1000, message = "项目描述不能超过 1000 个字符")
    private String description;

    private String script;

    /** 分镜 JSON 字符串 */
    private String storyboard;
}
