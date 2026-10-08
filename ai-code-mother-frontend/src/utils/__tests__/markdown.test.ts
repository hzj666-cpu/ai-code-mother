import { describe, expect, it } from 'vitest'
import { renderMarkdown } from '@/utils/markdown.ts'

describe('renderMarkdown', () => {
  it('渲染标题、列表等基础 Markdown', () => {
    const html = renderMarkdown('# 标题\n\n- 项目一\n- 项目二')
    expect(html).toContain('<h1>标题</h1>')
    expect(html).toContain('<li>项目一</li>')
  })

  it('HTML 代码块高亮：标记语言与 class 注入', () => {
    const html = renderMarkdown('```html\n<div class="box">hi</div>\n```')
    // 代码块渲染为 pre > code，且带 hljs 语言 class
    expect(html).toContain('<pre><code class="hljs language-html')
    // 标签名被高亮为 span
    expect(html).toContain('<span class="hljs-tag"')
  })

  it('CSS / JavaScript 代码块高亮', () => {
    const css = renderMarkdown('```css\nbody { margin: 0; }\n```')
    expect(css).toContain('language-css')
    expect(css).toContain('hljs-selector-tag')

    const js = renderMarkdown('```javascript\nconst a = 1;\n```')
    expect(js).toContain('language-javascript')
    expect(js).toContain('hljs-keyword')
  })

  it('未标记语言的代码块按纯文本渲染不报错', () => {
    const html = renderMarkdown('```\n纯文本内容\n```')
    expect(html).toContain('纯文本内容')
  })

  it('XSS 载荷被 DOMPurify 消毒', () => {
    const html = renderMarkdown('```\n<script>alert(1)</script>\n```')
    // 代码块内的 script 已被转义为文本，不会产生可执行的 script 节点
    expect(html).not.toContain('<script>alert(1)</script>')
    expect(html).not.toMatch(/<script>/)

    const evil = renderMarkdown('<img src=x onerror=alert(1)>')
    expect(evil).not.toContain('onerror')
  })

  it('空内容返回空字符串', () => {
    expect(renderMarkdown('')).toBe('')
  })
})
