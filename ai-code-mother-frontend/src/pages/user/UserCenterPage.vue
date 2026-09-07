<template>
  <div class="user-center-page">
    <a-card title="个人中心" class="user-center-card">
      <!-- 当前信息预览 -->
      <div class="user-center-preview">
        <a-avatar :size="64" :src="formState.userAvatar">
          {{ formState.userName?.charAt(0) }}
        </a-avatar>
        <div class="user-center-preview__meta">
          <div class="user-center-preview__name">{{ formState.userName }}</div>
          <a-tag v-if="formState.userRole === 'admin'" color="green">管理员</a-tag>
          <a-tag v-else color="blue">普通用户</a-tag>
        </div>
      </div>

      <a-divider />

      <!-- 编辑表单 -->
      <a-form
        layout="vertical"
        :model="formState"
        :rules="rules"
        ref="formRef"
        @finish="doUpdate"
      >
        <a-form-item label="账号（不可修改）" name="userAccount">
          <a-input :value="formState.userAccount" disabled />
        </a-form-item>
        <a-form-item label="用户名" name="userName">
          <a-input v-model:value="formState.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像地址" name="userAvatar">
          <a-input v-model:value="formState.userAvatar" placeholder="请输入头像图片地址" />
        </a-form-item>
        <a-form-item label="简介" name="userProfile">
          <a-textarea
            v-model:value="formState.userProfile"
            placeholder="请输入个人简介"
            :rows="3"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="updateLoading">
              保存
            </a-button>
            <a-button @click="resetForm">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { updateUser } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const loginUserStore = useLoginUserStore()
const formRef = ref<FormInstance>()

// 表单数据：从当前登录用户初始化
const formState = reactive({
  id: undefined as number | undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

const updateLoading = ref(false)

// 用当前登录用户信息回填表单
const initForm = () => {
  const loginUser = loginUserStore.loginUser
  formState.id = loginUser.id
  formState.userAccount = loginUser.userAccount ?? ''
  formState.userName = loginUser.userName ?? ''
  formState.userAvatar = loginUser.userAvatar ?? ''
  formState.userProfile = loginUser.userProfile ?? ''
  formState.userRole = loginUser.userRole ?? ''
}

// 重置为当前登录用户的最新值
const resetForm = () => {
  initForm()
  formRef.value?.clearValidate()
}

// 提交更新（只改自己的资料，不含 userRole——角色变更走用户管理）
const doUpdate = async () => {
  if (!formState.id) {
    message.warning('尚未登录')
    return
  }
  updateLoading.value = true
  try {
    const res = await updateUser({
      id: formState.id,
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('保存成功')
      // 同步更新全局登录态，头部头像/用户名即时刷新
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
      })
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } finally {
    updateLoading.value = false
  }
}

onMounted(() => {
  initForm()
})
</script>

<style scoped>
.user-center-page {
  display: flex;
  justify-content: center;
  padding: 48px 24px;
}

.user-center-card {
  width: 100%;
  max-width: 520px;
}

.user-center-preview {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-center-preview__meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-center-preview__name {
  font-size: 18px;
  font-weight: 600;
}
</style>
