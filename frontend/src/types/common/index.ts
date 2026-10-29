export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  page: number;
  size: number;
  total: number;
  records: T[];
}
