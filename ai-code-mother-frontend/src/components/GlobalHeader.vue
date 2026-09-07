<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import logoUrl from '@/assets/logo.png'
import { menuItems as defaultMenuItems, type MenuConfigItem } from '@/layouts/menuConfig'

const props = withDefaults(
  defineProps<{
    /** 网站标题 */
    title?: string
    /** 导航菜单配置 */
    items?: MenuConfigItem[]
  }>(),
  {
    title: 'AI 码农',
    items: () => [...defaultMenuItems],
  },
)

const route = useRoute()
const router = useRouter()

/** 当前选中菜单项跟随路由变化 */
const selectedKeys = computed(() => [route.path])

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key !== route.path) {
    router.push(String(key))
  }
}

import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
//用户注销
const doLogout =async () => {
  // TODO: 实现用户注销逻辑
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
    } else {
    message.error('退出登录失败'+res.data.message)
  }
}

// 跳转个人中心
const doUserCenter = () => {
  router.push('/user/center')
}
// JS 中引入 Store
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
const loginUserStore = useLoginUserStore()

/** 按登录用户角色过滤菜单：配置了 access 的菜单项，仅角色匹配的用户可见 */
const visibleItems = computed<MenuConfigItem[]>(() => {
  const userRole = loginUserStore.loginUser?.userRole
  return props.items.filter((item) => !item.access || item.access === userRole)
})
</script>

<template>
  <a-layout-header class="global-header">
    <div class="global-header__left">
      <img class="global-header__logo" :src="logoUrl" alt="logo" />
      <h1 class="global-header__title">{{ title }}</h1>
    </div>

    <a-menu
      class="global-header__menu"
      mode="horizontal"
      :items="visibleItems"
      :selected-keys="selectedKeys"
      @click="handleMenuClick"
    />

    <div class="user-login-status">
      <div v-if="loginUserStore.loginUser.id">
        <a-dropdown>
          <a-space>
            <a-avatar :src="loginUserStore.loginUser.userAvatar" />
            {{ loginUserStore.loginUser.userName ?? '无名' }}
          </a-space>
          <template #overlay>
            <a-menu>
              <a-menu-item key="userCenter" @click="doUserCenter">
                <UserOutlined />
                个人中心
              </a-menu-item>
              <a-menu-item key="logout" @click="doLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div v-else>
        <a-button type="primary" href="/user/login">登录</a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.global-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.global-header__logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.global-header__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.global-header__menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
  background: transparent;
}

.global-header__right {
  flex-shrink: 0;
}

/* 响应式：窄屏下隐藏标题、压缩间距（菜单超宽部分由 Menu 组件自动收纳） */
@media (max-width: 768px) {
  .global-header {
    gap: 12px;
    padding: 0 12px;
  }

  .global-header__title {
    display: none;
  }
}
</style>
