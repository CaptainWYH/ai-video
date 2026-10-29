import { http } from '@/api/request';
import type { PageResult } from '@/types/common';
import type { CreateProjectPayload, Project } from '@/types/project';

export const projectApi = {
  create(payload: CreateProjectPayload) {
    return http<Project>({
      url: '/projects',
      method: 'POST',
      data: payload,
    });
  },

  list(page = 1, size = 20) {
    return http<PageResult<Project>>({
      url: '/projects',
      method: 'GET',
      params: { page, size },
    });
  },

  get(projectId: number) {
    return http<Project>({
      url: `/projects/${projectId}`,
      method: 'GET',
    });
  },

  update(projectId: number, payload: CreateProjectPayload) {
    return http<Project>({
      url: `/projects/${projectId}`,
      method: 'PUT',
      data: payload,
    });
  },

  remove(projectId: number) {
    return http<void>({
      url: `/projects/${projectId}`,
      method: 'DELETE',
    });
  },
};
