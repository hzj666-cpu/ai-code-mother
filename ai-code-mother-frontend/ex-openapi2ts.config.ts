import type { GenerateServiceProps } from '@umijs/openapi'

/**
 * openapi2ts 配置
 * 后端启动后执行 npm run openapi2ts，根据 OpenAPI 文档自动生成接口请求代码
 */
const config: GenerateServiceProps = {
  /** 后端 OpenAPI 文档地址（对应后端 server.port=8123 + context-path=/api） */
  schemaPath: 'http://localhost:8123/api/v3/api-docs/default',
  /** 生成代码的目录 */
  serversPath: './src/services',
  /** 项目名称，作为生成的命名空间（API.xxx） */
  projectName: 'api',
  /** 接口前缀：后端 context-path 为 /api，dev 环境由 Vite 代理转发 */
  apiPrefix: '"/api"',
  /** 请求库导入语句，指向本地封装的 axios 请求器 */
  requestLibPath: `import request from '@/utils/request'`,
}

export default config
