<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import { menuItems } from './menuConfig'

withDefaults(
  defineProps<{
    /** 网站标题，透传给 GlobalHeader */
    title?: string
  }>(),
  {
    title: 'AI 码农',
  },
)
</script>

<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader :title="title" :items="menuItems" />

    <!-- 中间内容区域：根据路由切换页面 -->
    <a-layout-content class="basic-layout__content">
      <RouterView />
    </a-layout-content>

    <!-- 底部版权信息：内容不足一屏时也始终贴底 -->
    <GlobalFooter />
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.basic-layout__content {
  flex: 1;
  padding: 16px;
}

@media (min-width: 768px) {
  .basic-layout__content {
    padding: 24px;
  }
}
</style>
