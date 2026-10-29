package com.aivideo.project.controller;

import com.aivideo.common.PageResult;
import com.aivideo.common.Result;
import com.aivideo.project.dto.CreateProjectRequest;
import com.aivideo.project.dto.ProjectResponse;
import com.aivideo.project.service.ProjectService;
import com.aivideo.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "创作项目", description = "剧本、分镜等项目元数据")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "创建项目")
    @PostMapping
    public Result<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return Result.ok(projectService.create(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "项目列表（分页）")
    @GetMapping
    public Result<PageResult<ProjectResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(projectService.list(SecurityUtils.currentUserId(), page, size));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public Result<ProjectResponse> get(@PathVariable("id") Long projectId) {
        return Result.ok(projectService.get(SecurityUtils.currentUserId(), projectId));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    public Result<ProjectResponse> update(@PathVariable("id") Long projectId,
                                          @Valid @RequestBody CreateProjectRequest request) {
        return Result.ok(projectService.update(SecurityUtils.currentUserId(), projectId, request));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long projectId) {
        projectService.delete(SecurityUtils.currentUserId(), projectId);
        return Result.ok();
    }
}
