<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>

    <a-divider />
    <!-- 表格 -->

    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" @click="doEdit(record)">编辑</a-button>
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑用户弹窗 -->
    <a-modal
      v-model:open="editModalOpen"
      title="编辑用户"
      :confirm-loading="updateLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="doUpdate"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="账号（不可修改）">
          <a-input :value="editingUser.userAccount" disabled />
        </a-form-item>
        <a-form-item label="用户名" required>
          <a-input v-model:value="editingUser.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像地址">
          <a-input v-model:value="editingUser.userAvatar" placeholder="请输入头像图片地址" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea
            v-model:value="editingUser.userProfile"
            placeholder="请输入用户简介"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="editingUser.userRole" placeholder="请选择角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { SmileOutlined, DownOutlined } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage, updateUser } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 数据
const data = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}
// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 获取数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}
// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

// 编辑用户弹窗状态（userAccount 仅用于展示，提交时不传）
const editModalOpen = ref(false)
const updateLoading = ref(false)
const editingUser = ref<API.UserUpdateRequest & { userAccount?: string }>({})

// 点击编辑：用当前行数据回填表单
const doEdit = (record: API.UserVO) => {
  editingUser.value = {
    id: record.id,
    userAccount: record.userAccount,
    userName: record.userName,
    userAvatar: record.userAvatar,
    userProfile: record.userProfile,
    userRole: record.userRole,
  }
  editModalOpen.value = true
}

// 提交更新
const doUpdate = async () => {
  const { id, userName, userAvatar, userProfile, userRole } = editingUser.value
  if (!id) {
    return
  }
  if (!userName || !userName.trim()) {
    message.warning('用户名不能为空')
    return
  }
  updateLoading.value = true
  try {
    const res = await updateUser({ id, userName, userAvatar, userProfile, userRole })
    if (res.data.code === 0 && res.data.data) {
      message.success('更新成功')
      editModalOpen.value = false
      // 刷新数据
      fetchData()
    } else {
      message.error('更新失败，' + res.data.message)
    }
  } finally {
    updateLoading.value = false
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>
