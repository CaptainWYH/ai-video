package com.aivideo.project.service.impl;

import com.aivideo.common.BizException;
import com.aivideo.common.PageResult;
import com.aivideo.project.dto.CreateProjectRequest;
import com.aivideo.project.dto.ProjectResponse;
import com.aivideo.project.entity.Project;
import com.aivideo.project.mapper.ProjectMapper;
import com.aivideo.project.service.ProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public ProjectResponse create(Long userId, CreateProjectRequest request) {
        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setScript(request.getScript());
        project.setStoryboard(request.getStoryboard());
        projectMapper.insert(project);
        return ProjectResponse.from(project);
    }

    @Override
    public PageResult<ProjectResponse> list(Long userId, long page, long size) {
        Page<Project> pageRequest = new Page<>(Math.max(1, page), Math.min(MAX_PAGE_SIZE, Math.max(1, size)));
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .orderByDesc(Project::getId);
        Page<Project> result = projectMapper.selectPage(pageRequest, wrapper);
        List<ProjectResponse> records = result.getRecords().stream()
                .map(ProjectResponse::from)
                .toList();
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public ProjectResponse get(Long userId, Long projectId) {
        return ProjectResponse.from(getOwnedProject(userId, projectId));
    }

    @Override
    @Transactional
    public ProjectResponse update(Long userId, Long projectId, CreateProjectRequest request) {
        Project project = getOwnedProject(userId, projectId);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setScript(request.getScript());
        project.setStoryboard(request.getStoryboard());
        projectMapper.updateById(project);
        return ProjectResponse.from(project);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long projectId) {
        getOwnedProject(userId, projectId);
        projectMapper.deleteById(projectId);
    }

    private Project getOwnedProject(Long userId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !project.getUserId().equals(userId)) {
            throw BizException.notFound("项目不存在");
        }
        return project;
    }
}
