import { http } from '@/api/request';
import type { PageResult } from '@/types/common';
import type { CreateVideoTaskPayload, VideoTask, VideoUsageSummary } from '@/types/video';

export const videoApi = {
  createTask(payload: CreateVideoTaskPayload) {
    return http<VideoTask>({
      url: '/video/tasks',
      method: 'POST',
      data: payload,
    });
  },

  getTask(taskId: number) {
    return http<VideoTask>({
      url: `/video/tasks/${taskId}`,
      method: 'GET',
    });
  },

  listTasks(page = 1, size = 20) {
    return http<PageResult<VideoTask>>({
      url: '/video/tasks',
      method: 'GET',
      params: { page, size },
    });
  },

  retryTask(taskId: number) {
    return http<VideoTask>({
      url: `/video/tasks/${taskId}/retry`,
      method: 'POST',
    });
  },

  cancelTask(taskId: number) {
    return http<VideoTask>({
      url: `/video/tasks/${taskId}`,
      method: 'DELETE',
    });
  },

  usageSummary(from?: string, to?: string) {
    return http<VideoUsageSummary>({
      url: '/video/usage/summary',
      method: 'GET',
      params: { from, to },
    });
  },
};
