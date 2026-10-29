import { http } from '@/api/request';
import type { PageResult } from '@/types/common';
import type { CreateVideoTaskPayload, VideoTask } from '@/types/video';

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
};
