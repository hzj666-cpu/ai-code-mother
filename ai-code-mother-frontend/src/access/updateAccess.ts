import router from '@/router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import AccessEnum from '@/access/accessEnum'
import checkAccess from '@/access/checkAccess'

// 是否为首次获取登录用户（保证刷新页面时先等后端返回用户信息再校验权限）
let firstFetchLoginUser = true

/**
 * 注册全局权限路由守卫：
 * 1. 首次导航时拉取登录用户，确保刷新后权限校验有据可依
 * 2. 根据 to.meta.access 中声明的所需权限，自动拦截：
 *    - 需要登录但未登录 → 跳登录页（带 redirect 回跳参数）
 *    - 已登录但角色不足 → 跳无权限页
 * 3. 已登录用户访问登录/注册页 → 按角色跳首页（管理员跳用户管理）
 */
export function registerAccessGuard() {
  router.beforeEach(async (to, from, next) => {
    const loginUserStore = useLoginUserStore()
    let loginUser = loginUserStore.loginUser

    // 页面刷新后的首次导航，等待后端返回登录用户信息
    if (firstFetchLoginUser) {
      await loginUserStore.fetchLoginUser()
      loginUser = loginUserStore.loginUser
      firstFetchLoginUser = false
    }

    // 已登录用户访问登录/注册页，按角色跳转到首页
    if (['/user/login', '/user/register'].includes(to.path) && loginUser.id) {
      next(loginUser.userRole === AccessEnum.ADMIN ? '/admin/userManage' : '/')
      return
    }

    // 读取路由 meta 中声明的所需权限，未声明则视为无需登录
    const needAccess = (to.meta?.access as AccessEnum) ?? AccessEnum.NOT_LOGIN

    // 需要登录但当前未登录 → 跳登录页
    if (needAccess !== AccessEnum.NOT_LOGIN && !loginUser.id) {
      message.warning('请先登录')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }

    // 权限不足 → 跳无权限页
    if (!checkAccess(loginUser, needAccess)) {
      message.error('没有权限')
      next('/noAuth')
      return
    }

    next()
  })
}
