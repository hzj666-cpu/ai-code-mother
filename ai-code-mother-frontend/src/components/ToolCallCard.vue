<template>
  <div class="tool-card" :class="{ 'is-failed': status === 'failed' }">
    <div class="tool-card-header" @click="toggle">
      <span class="tool-badge">[工具调用] {{ toolLabel }}</span>
      <span class="tool-file">{{ tool.filePath || tool.name }}</span>
      <span class="tool-status" :title="tool.result">
        <LoadingOutlined v-if="status === 'running'" spin class="status-running" />
        <CheckCircleFilled v-else-if="status === 'done'" class="status-ok" />
        <CloseCircleFilled v-else class="status-fail" />
      </span>
      <RightOutlined v-if="tool.content" class="expand-arrow" :class="{ expanded }" />
    </div>
    <div v-if="status === 'failed' && tool.result" class="tool-card-error">{{ tool.result }}</div>
    <pre v-if="expanded && highlighted" class="tool-code"><code v-html="highlighted"></code></pre>
  </div>
</template>

<script lang="ts">
/**
 * 工具调用事件（SSE tool_request/tool_executed 及历史 tool 记录的统一形状）
 */
export interface ToolEvent {
  key: string
  name: string
  /** 调用参数中解析出的文件相对路径（如 src/App.vue） */
  filePath?: string
  /** 调用参数中解析出的完整文件内容 */
  content?: string
  /** 工具执行结果文本 */
  result?: string
  failed?: boolean
  /** 显式状态；缺省时按 failed/result 推断（兼容历史旧记录） */
  status?: 'running' | 'done' | 'failed'
}
</script>

<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  LoadingOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  RightOutlined,
} from '@ant-design/icons-vue'
// 按语言包按需注册，控制体积；highlight.js 11.x 无独立 vue 语言包，.vue 按 xml 高亮
// （SFC 主体是 HTML 模板，xml 高亮覆盖度已足够）
import hljs from 'highlight.js/lib/core'
import xml from 'highlight.js/lib/languages/xml'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import 'highlight.js/styles/github.css'

hljs.registerLanguage('xml', xml)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('css', css)
hljs.registerLanguage('json', json)

const props = defineProps<{ tool: ToolEvent }>()

// 默认折叠：一轮生成 8~10 个文件，全部展开会把聊天区撑爆
const expanded = ref(false)
const toggle = () => {
  if (props.tool.content) expanded.value = !expanded.value
}

// 工具名中文映射；后续新增工具在此补一行
const TOOL_NAME_LABELS: Record<string, string> = {
  writeFile: '写入文件',
}
const toolLabel = computed(() => TOOL_NAME_LABELS[props.tool.name] ?? props.tool.name)

const status = computed<'running' | 'done' | 'failed'>(() => {
  if (props.tool.status) return props.tool.status
  if (props.tool.failed) return 'failed'
  return props.tool.result ? 'done' : 'running'
})

// 按文件扩展名选高亮语言；识别不了时交给 highlightAuto
const language = computed(() => {
  const filePath = props.tool.filePath ?? ''
  if (filePath.endsWith('.vue')) return 'xml'
  if (filePath.endsWith('.ts')) return 'typescript'
  if (filePath.endsWith('.js')) return 'javascript'
  if (filePath.endsWith('.css')) return 'css'
  if (filePath.endsWith('.json')) return 'json'
  if (filePath.endsWith('.html') || filePath.endsWith('.xml')) return 'xml'
  return ''
})

const highlighted = computed(() => {
  const content = props.tool.content
  if (!content) return ''
  try {
    // hljs.highlight 会转义输入中的 HTML，v-html 注入是安全的
    if (language.value) {
      return hljs.highlight(content, { language: language.value, ignoreIllegals: true }).value
    }
    return hljs.highlightAuto(content).value
  } catch {
    const el = document.createElement('div')
    el.textContent = content
    return el.innerHTML
  }
})
</script>

<style scoped>
.tool-card {
  margin-top: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8f9fb;
  overflow: hidden;
}

.tool-card.is-failed {
  border-color: #ffa39e;
  background: #fff2f0;
}

.tool-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 12px;
  color: #4a5568;
  overflow-wrap: anywhere;
}

.tool-card-header:hover {
  background: #eef1f6;
}

.tool-badge {
  flex-shrink: 0;
  color: #1f6feb;
  font-weight: 600;
}

.tool-file {
  flex: 1;
  font-family: Consolas, Monaco, monospace;
  color: #24292f;
}

.tool-status {
  flex-shrink: 0;
}

.status-running {
  color: #1890ff;
}

.status-ok {
  color: #52c41a;
}

.status-fail {
  color: #ff4d4f;
}

.expand-arrow {
  flex-shrink: 0;
  font-size: 10px;
  color: #9ca3af;
  transition: transform 0.2s;
}

.expand-arrow.expanded {
  transform: rotate(90deg);
}

.tool-card-error {
  padding: 6px 12px;
  font-size: 12px;
  color: #cf1322;
  background: #fff1f0;
  border-top: 1px solid #ffa39e;
}

.tool-code {
  margin: 0;
  padding: 12px;
  max-height: 420px;
  overflow: auto;
  background: #f6f8fa;
  border-top: 1px solid #e5e7eb;
  font-size: 12px;
  line-height: 1.5;
}

.tool-code code {
  font-family: Consolas, Monaco, monospace;
}
</style>
