import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

/**
 * 轻量请求封装：与 openapi2ts 生成代码的 request(url, options) 签名对齐，
 * 直接返回响应体数据（res.data），泛型 T 为后端 BaseResponse 结构
 */
const request = async <T = unknown>(
  url: string,
  config: AxiosRequestConfig = {},
): Promise<T> => {
  const res = await axios.request<T>({ url, ...config })
  return res.data
}

export default request
