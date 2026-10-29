import axios from 'axios';
import { ApiError } from '@/api/request';

/**
 * 将后端 Result / Axios / Error 统一转换成可展示文案。
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined;
    return data?.message ?? error.message ?? '网络请求失败';
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '操作失败，请稍后重试';
}
