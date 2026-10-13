<template>
  <div id="chatHistoryManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用 ID">
        <a-input-number v-model:value="searchParams.appId" placeholder="应用 ID" :min="1" />
      </a-form-item>
      <a-form-item label="用户 ID">
        <a-input-number v-model:value="searchParams.userId" placeholder="用户 ID" :min="1" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select v-model:value="searchParams.messageType" placeholder="全部" allow-clear style="width: 130px">
          <a-select-option value="user">用户</a-select-option>
          <a-select-option value="ai">AI</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="消息内容">
        <a-input v-model:value="searchParams.message" placeholder="搜索消息内容" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="false"
      :loading="loading"
      row-key="id"
      :scroll="{ x: 1100 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'messageType'">
          <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
            {{ record.messageType === 'user' ? '用户' : 'AI' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          <a-tooltip :title="record.message">
            <div class="message-text">{{ record.message || '（空消息）' }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-popconfirm title="确定删除这条对话记录吗？" @confirm="deleteRecord(record.id)">
            <a-button danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <div class="cursor-pagination">
      <span>第 {{ pageIndex + 1 }} 批，共 {{ data.length }} 条</span>
      <a-space>
        <a-select v-model:value="pageSize" style="width: 96px" @change="changePageSize">
          <a-select-option :value="10">10 条/页</a-select-option>
          <a-select-option :value="20">20 条/页</a-select-option>
          <a-select-option :value="50">50 条/页</a-select-option>
        </a-select>
        <a-button :disabled="pageIndex === 0 || loading" @click="previousPage">上一页</a-button>
        <a-button type="primary" :disabled="!hasMore || loading" @click="nextPage">下一页</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { deleteChatHistory, listAllChatHistoryVoByPage } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'

type Cursor = { beforeCreateTime?: string; beforeId?: number }

const columns = [
  { title: '记录 ID', dataIndex: 'id', width: 90 },
  { title: '应用 ID', dataIndex: 'appId', width: 100 },
  { title: '用户 ID', dataIndex: 'userId', width: 100 },
  { title: '消息类型', dataIndex: 'messageType', width: 100 },
  { title: '消息内容', dataIndex: 'message', width: 480 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' },
]

const data = ref<API.ChatHistoryVO[]>([])
const loading = ref(false)
const hasMore = ref(false)
const nextCursor = ref<Cursor>({})
const pageSize = ref(10)
const pageIndex = ref(0)
const cursorStack = ref<Cursor[]>([{}])
const searchParams = reactive<API.ChatHistoryQueryRequest>({})

const fetchData = async () => {
  loading.value = true
  try {
    const cursor = cursorStack.value[pageIndex.value] ?? {}
    const res = await listAllChatHistoryVoByPage({
      ...searchParams,
      pageSize: pageSize.value,
      ...cursor,
    })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('获取对话记录失败：' + (res.data.message || '未知错误'))
      return
    }
    data.value = res.data.data.records ?? []
    hasMore.value = res.data.data.hasMore ?? false
    nextCursor.value = {
      beforeCreateTime: res.data.data.nextBeforeCreateTime,
      beforeId: res.data.data.nextBeforeId,
    }
  } catch (error) {
    console.error('获取对话记录失败：', error)
    message.error('获取对话记录失败')
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  pageIndex.value = 0
  cursorStack.value = [{}]
  fetchData()
}

const nextPage = () => {
  if (!hasMore.value || loading.value) return
  if (!nextCursor.value.beforeCreateTime || nextCursor.value.beforeId === undefined) return
  cursorStack.value = cursorStack.value.slice(0, pageIndex.value + 1)
  cursorStack.value.push({ ...nextCursor.value })
  pageIndex.value += 1
  fetchData()
}

const previousPage = () => {
  if (pageIndex.value === 0 || loading.value) return
  pageIndex.value -= 1
  fetchData()
}

const changePageSize = () => doSearch()

const deleteRecord = async (id?: number) => {
  if (id === undefined) return
  try {
    const res = await deleteChatHistory({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      await fetchData()
    } else {
      message.error('删除失败：' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('删除对话记录失败：', error)
    message.error('删除失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
#chatHistoryManagePage {
  padding: 24px;
  margin-top: 16px;
  background: white;
}

.message-text {
  max-width: 480px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cursor-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
</style>
