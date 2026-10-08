import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
// 只引入常用语言子集（含 xml/html、css、javascript、json、typescript 等），
// 比全量 highlight.js 小很多
import hljs from 'highlight.js/lib/common'
import DOMPurify from 'dompurify'

/**
 * Markdown 渲染器：
 * - marked 解析（主流 Markdown 解析库）
 * - marked-highlight + highlight.js 做 HTML/CSS/JavaScript 等代码高亮
 * - DOMPurify 消毒：AI 输出属于不可信内容，必须过滤脚本/事件等 XSS 载荷后才能进 v-html
 */
const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      // 语言不认识（含未标记语言的代码块）时按纯文本处理，hljs.highlight 对空语言会抛错
      const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
      return hljs.highlight(code, { language }).value
    },
  }),
)

/**
 * 将 Markdown 文本渲染为可安全用于 v-html 的 HTML 字符串
 * @param markdown Markdown 原文（如 AI 的流式回复）
 * @return 消毒后的 HTML
 */
export function renderMarkdown(markdown: string): string {
  if (!markdown) {
    return ''
  }
  return DOMPurify.sanitize(marked.parse(markdown) as string)
}
