<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined/>
          </template>
          应用详情
        </a-button>
        <a-button
          type="primary"
          ghost
          @click="downloadCode"
          :loading="downloading"
          :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined/>
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined/>
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">
                <div>{{ message.content }}</div>
                <div v-if="message.assets?.length" class="message-assets">
                  <img v-for="asset in message.assets" :key="asset.assetId"
                       :src="assetPreviewUrl(asset.previewUrl)" :alt="asset.purpose === 'REFERENCE' ? '参考图片' : '网站素材'" />
                </div>
              </div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar"/>
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar"/>
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content"/>
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small"/>
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div v-if="pendingAssets.length" class="pending-assets">
            <div v-for="asset in pendingAssets" :key="asset.assetId" class="pending-asset">
              <img :src="assetPreviewUrl(asset.previewUrl)" alt="待发送图片" />
              <span>{{ asset.purpose === 'REFERENCE' ? '参考图' : '网站素材' }}</span>
              <a-button type="link" size="small" :disabled="isGenerating" @click="removePendingAsset(asset.assetId)">移除</a-button>
            </div>
          </div>
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-select v-model:value="uploadPurpose" size="small" :disabled="isGenerating || uploadingCount > 0" style="width: 110px">
                <a-select-option value="REFERENCE">参考图片</a-select-option>
                <a-select-option value="SITE_ASSET">网站素材</a-select-option>
              </a-select>
              <a-upload accept="image/png,image/jpeg" multiple :show-upload-list="false" :before-upload="beforeImageUpload">
                <a-button size="small" :loading="uploadingCount > 0" :disabled="!isOwner || isGenerating || pendingAssets.length >= 4">上传图片</a-button>
              </a-upload>
              <a-button
                v-if="isOwner && previewUrl"
                :type="isEditMode ? 'primary' : 'default'"
                :danger="isEditMode"
                class="visual-edit-button"
                :disabled="isGenerating"
                @click="toggleEditMode"
              >
                <template #icon><EditOutlined /></template>
                {{ isEditMode ? '退出编辑' : '可视化编辑' }}
              </a-button>
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner || uploadingCount > 0"
              >
                <template #icon>
                  <SendOutlined/>
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined/>
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="previewError && !isGenerating" class="preview-placeholder">
            <p>{{ previewError }}</p>
          </div>
          <div v-else-if="isBuilding && !isGenerating" class="preview-loading">
            <a-spin size="large"/>
            <p>Vue 工程正在构建，完成后自动显示...</p>
          </div>
          <div v-else-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large"/>
            <p>正在生成网站...</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, nextTick, onUnmounted, computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {message} from 'ant-design-vue'
import {useLoginUserStore} from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteMyApp as deleteAppApi,
} from '@/api/appController'
import { listChatHistoryVoByPage } from '@/api/chatHistoryController'
import {CodeGenTypeEnum, formatCodeGenType} from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import {API_BASE_URL, getStaticPreviewUrl} from '@/config/env'
import {VisualEditor, type ElementInfo} from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  assets?: ImageAsset[]
}

interface ImageAsset {
  assetId: string
  purpose: 'REFERENCE' | 'SITE_ASSET'
  previewUrl: string
  sitePath?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const uploadPurpose = ref<ImageAsset['purpose']>('REFERENCE')
const uploadingCount = ref(0)
const pendingAssets = ref<ImageAsset[]>([])
const assetPreviewUrl = (path: string) => `${API_BASE_URL.replace(/\/$/, '')}${path}`
const removePendingAsset = async (id: string) => {
  try {
    await request.post(`/app/assets/${id}/delete`)
  } catch (error) {
    console.warn('删除未发送图片失败：', error)
  }
  pendingAssets.value = pendingAssets.value.filter((asset) => asset.assetId !== id)
}
const beforeImageUpload = (file: File) => {
  if (!appId.value || !isOwner.value || pendingAssets.value.length + uploadingCount.value >= 4) {
    message.warning('每轮最多添加 4 张图片')
    return false
  }
  if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    message.error('请选择不超过 5 MB 的 PNG 或 JPEG 图片')
    return false
  }
  const purpose = uploadPurpose.value
  uploadingCount.value++
  const form = new FormData()
  form.append('file', file)
  form.append('purpose', purpose)
  void request.post<{ code: number; message?: string; data?: ImageAsset }>(
    `/app/${appId.value}/assets`, form,
  ).then((res) => {
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '上传失败')
    pendingAssets.value.push(res.data.data)
  }).catch((error) => {
    console.error('上传图片失败：', error)
    message.error('图片上传失败，请重试')
  }).finally(() => { uploadingCount.value-- })
  return false
}
const messagesContainer = ref<HTMLElement>()
let streamAbortController: AbortController | null = null

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const previewError = ref('')
const isBuilding = ref(false)
let buildPollTimer: ReturnType<typeof setTimeout> | undefined
let buildPollGeneration = 0
let buildPollFailures = 0
let buildPollStartedAt = 0

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.ChatHistoryQueryRequest = {
      // 雪花 ID 超出 JS 安全整数范围，保留路由字符串，避免 Number 转换造成精度丢失。
      appId: appId.value as unknown as number,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.beforeCreateTime = lastCreateTime.value
    }
    const res = await listChatHistoryVoByPage(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.message || '',
            createTime: chat.createTime,
            assets: (chat.assets || []) as ImageAsset[],
          }))
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({id: id as unknown as number})
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      syncPreview()
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if ((!userInput.value.trim() && !pendingAssets.value.length) || isGenerating.value || uploadingCount.value > 0) {
    return
  }

  let message = userInput.value.trim() || '请根据上传的图片修改网站'
  const assets = [...pendingAssets.value]
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
    assets,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex, assets)
}

// 生成代码：EventSource 只支持 GET；此接口是 POST JSON，因此使用 fetch 读取 SSE 流。
const generateCode = async (userMessage: string, aiMessageIndex: number, assets: ImageAsset[] = []) => {
  let streamCompleted = false
  const controller = new AbortController()
  streamAbortController = controller
  stopBuildPolling()
  isBuilding.value = false
  buildPollFailures = 0
  buildPollStartedAt = 0
  previewError.value = ''

  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const response = await fetch(`${baseURL}/app/chat/gen/code`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      credentials: 'include',
      signal: controller.signal,
      body: JSON.stringify({
        appId: String(appId.value),
        chatPrompt: userMessage,
        assetIds: assets.map((asset) => asset.assetId),
      }),
    })

    if (!response.ok) {
      const responseText = await response.text()
      throw new Error(responseText || `生成请求失败（HTTP ${response.status}）`)
    }
    if (!response.body) {
      throw new Error('浏览器未提供流式响应读取能力')
    }

    let fullContent = ''
    const appendMessageData = (data: string) => {
      if (!data) return
      let content = data
      try {
        const parsed = JSON.parse(data)
        if (typeof parsed?.data === 'string') {
          content = parsed.data
        } else if (typeof parsed?.d === 'string') {
          // 兼容旧版 SSE 包装格式。
          content = parsed.d
        }
      } catch {
        // HTML / 多文件模式直接发送文本 token，不是 JSON。
      }

      if (!content) return
      fullContent += content
      messages.value[aiMessageIndex].content = fullContent
      messages.value[aiMessageIndex].loading = false
      scrollToBottom()
    }

    const dispatchSseEvent = (block: string) => {
      let eventName = 'message'
      const dataLines: string[] = []
      for (const line of block.split(/\r?\n/)) {
        if (!line || line.startsWith(':')) continue
        const separator = line.indexOf(':')
        const field = separator < 0 ? line : line.slice(0, separator)
        const value = separator < 0 ? '' : line.slice(separator + 1).replace(/^ /, '')
        if (field === 'event') eventName = value
        if (field === 'data') dataLines.push(value)
      }

      const data = dataLines.join('\n')
      if (eventName === 'error') {
        throw new Error(data || '服务端生成失败')
      }
      if (eventName === 'done') {
        streamCompleted = true
        isGenerating.value = false
        if (assets.length) pendingAssets.value = pendingAssets.value.filter(
          (asset) => !assets.some((sent) => sent.assetId === asset.assetId),
        )
        void fetchAppInfo()
        return
      }
      appendMessageData(data)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (!streamCompleted) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let separatorIndex = buffer.search(/\r?\n\r?\n/)
      while (separatorIndex >= 0) {
        const separatorMatch = buffer.slice(separatorIndex).match(/^\r?\n\r?\n/)
        const separatorLength = separatorMatch?.[0].length ?? 2
        const block = buffer.slice(0, separatorIndex)
        buffer = buffer.slice(separatorIndex + separatorLength)
        dispatchSseEvent(block)
        if (streamCompleted) break
        separatorIndex = buffer.search(/\r?\n\r?\n/)
      }
    }
    buffer += decoder.decode()
    if (!streamCompleted && buffer.trim()) dispatchSseEvent(buffer)
    if (!streamCompleted) throw new Error('生成连接已中断，未收到完成事件')
  } catch (error) {
    if (!controller.signal.aborted) {
      console.error('生成代码失败：', error)
      handleError(error, aiMessageIndex)
    }
  } finally {
    if (streamAbortController === controller) streamAbortController = null
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

const stopBuildPolling = () => {
  if (buildPollTimer) clearTimeout(buildPollTimer)
  buildPollTimer = undefined
  buildPollGeneration++
}

const syncPreview = () => {
  if (!appId.value || !appInfo.value) return
  const {codeGenType, buildStatus, buildError} = appInfo.value
  if (codeGenType !== CodeGenTypeEnum.VUE_PROJECT) {
    if (messages.value.length >= 2) previewUrl.value = getStaticPreviewUrl(codeGenType || CodeGenTypeEnum.HTML, appId.value)
    return
  }
  isBuilding.value = buildStatus === 'PENDING' || buildStatus === 'BUILDING'
  previewError.value = buildStatus === 'FAILED' ? (buildError || 'Vue 工程构建失败，请检查后端构建日志') : ''
  if (buildStatus === 'SUCCESS') {
    // A new build uses the same URL; force the iframe to load the latest artifact.
    previewUrl.value = `${getStaticPreviewUrl(codeGenType, appId.value)}?t=${Date.now()}`
    previewReady.value = false
  } else {
    previewUrl.value = ''
  }
  if (isBuilding.value) {
    if (!buildPollStartedAt) buildPollStartedAt = Date.now()
    scheduleBuildPoll()
  } else {
    buildPollStartedAt = 0
    stopBuildPolling()
  }
}

const scheduleBuildPoll = () => {
  stopBuildPolling()
  const generation = buildPollGeneration
  buildPollTimer = setTimeout(async () => {
    try {
      const res = await getAppVoById({id: appId.value as unknown as number})
      if (generation !== buildPollGeneration) return
      if (res.data.code !== 0 || !res.data.data) throw new Error('获取构建状态失败')
      buildPollFailures = 0
      appInfo.value = res.data.data
      if (res.data.data.buildStatus === 'BUILDING' && Date.now() - buildPollStartedAt > 9 * 60 * 1000) {
        isBuilding.value = false
        previewError.value = '构建等待超时，请检查后端是否中途重启，然后重新生成'
        stopBuildPolling()
        return
      }
      syncPreview()
    } catch (error) {
      if (generation !== buildPollGeneration) return
      console.error('查询 Vue 构建状态失败：', error)
      if (++buildPollFailures >= 3) {
        isBuilding.value = false
        previewError.value = '无法查询构建状态，请确认后端服务已启动并刷新页面'
        stopBuildPolling()
      } else if (isBuilding.value) scheduleBuildPoll()
    }
  }, 2000)
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({id: appInfo.value.id})
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
  window.addEventListener('message', handleVisualEditorMessage)
})

// 清理资源
onUnmounted(() => {
  streamAbortController?.abort()
  stopBuildPolling()
  window.removeEventListener('message', handleVisualEditorMessage)
  visualEditor.destroy()
})

const handleVisualEditorMessage = (event: MessageEvent) => {
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement | null
  if (!iframe || event.source !== iframe.contentWindow || event.origin !== window.location.origin) return
  visualEditor.handleIframeMessage(event)
}
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fdfdfd;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 0.9;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
}

.pending-assets,
.message-assets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.pending-asset {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.pending-asset img,
.message-assets img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
}


.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: static;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  margin-top: 8px;
  align-items: center;
  gap: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}
</style>
