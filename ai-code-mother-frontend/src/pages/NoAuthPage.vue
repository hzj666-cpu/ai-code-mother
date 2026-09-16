<template>
  <div class="no-auth-page">
    <a-result status="403" title="403" sub-title="抱歉，您没有权限访问此页面。">
      <template #extra>
        <a-button type="primary" href="/">返回首页</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.no-auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 64px);
}
</style>
