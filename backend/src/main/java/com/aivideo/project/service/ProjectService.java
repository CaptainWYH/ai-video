package com.aivideo.project.service;

import com.aivideo.common.PageResult;
import com.aivideo.project.dto.CreateProjectRequest;
import com.aivideo.project.dto.ProjectResponse;

public interface ProjectService {

    ProjectResponse create(Long userId, CreateProjectRequest request);

    PageResult<ProjectResponse> list(Long userId, long page, long size);

    ProjectResponse get(Long userId, Long projectId);

    ProjectResponse update(Long userId, Long projectId, CreateProjectRequest request);

    void delete(Long userId, Long projectId);
}
