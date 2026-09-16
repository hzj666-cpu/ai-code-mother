import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserCenterPage from '@/pages/user/UserCenterPage.vue'
import NoAuthPage from '@/pages/NoAuthPage.vue'
import AccessEnum from '@/access/accessEnum'

declare module 'vue-router' {
  interface RouteMeta {
    /** 访问该页面所需的权限，不配置则无需登录 */
    access?: AccessEnum
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/user/center',
      name: '个人中心',
      component: UserCenterPage,
      // 需要登录后才能访问
      meta: {
        access: AccessEnum.USER,
      },
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
      // 仅管理员可访问
      meta: {
        access: AccessEnum.ADMIN,
      },
    },
    {
      path: '/noAuth',
      name: '无权限',
      component: NoAuthPage,
    },
  ],
})

export default router
