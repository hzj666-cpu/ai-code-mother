// @ts-ignore
/* eslint-disable */
import request from '@/utils/request'

/** 健康检查 GET /health/ */
export async function health(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>(`/api/health/`, {
    method: 'GET',
    ...(options || {}),
  })
}
