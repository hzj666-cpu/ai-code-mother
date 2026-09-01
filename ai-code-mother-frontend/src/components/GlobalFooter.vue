<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 版权信息文案 */
    copyright?: string
  }>(),
  {
    copyright: '拉丽原创项目 by 程序员拉丽',
  },
)
</script>

<template>
  <a-layout-footer class="global-footer">
    {{ copyright }}
  </a-layout-footer>
</template>

<style scoped>
.global-footer {
  padding: 16px 24px;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  background: transparent;
}
</style>
