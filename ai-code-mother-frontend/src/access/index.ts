/**
 * 权限管理模块入口：引入即生效（在 main.ts 中 import '@/access'）
 * 包含：权限枚举、权限校验、全局路由守卫
 */
import { registerAccessGuard } from '@/access/updateAccess'

registerAccessGuard()
