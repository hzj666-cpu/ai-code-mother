<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const submitting = ref(false)

const onFinish = async () => {
  submitting.value = true
  try {
    const res = await userLogin(formState)
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      message.success('登录成功，欢迎回来')
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } else {
      message.error('登录失败：' + (res.data.message ?? '账号或密码错误'))
    }
  } catch (e) {
    message.error('登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// ─── 粒子星网动画 ───────────────────────────────────────────
const canvasRef = ref<HTMLCanvasElement | null>(null)
let rafId = 0

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  r: number
}

const initParticles = (canvas: HTMLCanvasElement): Particle[] => {
  const count = Math.min(90, Math.floor((canvas.width * canvas.height) / 16000))
  return Array.from({ length: count }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    vx: (Math.random() - 0.5) * 0.35,
    vy: (Math.random() - 0.5) * 0.35,
    r: Math.random() * 1.8 + 0.6,
  }))
}

const drawParticles = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dpr = window.devicePixelRatio || 1
  const w = canvas.clientWidth
  const h = canvas.clientHeight
  if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
    canvas.width = w * dpr
    canvas.height = h * dpr
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }

  ctx.clearRect(0, 0, w, h)
  const particles = initParticles(canvas)
  const LINK_DIST = 130

  const render = () => {
    ctx.clearRect(0, 0, w, h)

    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(120, 200, 255, 0.75)'
      ctx.fill()
    }

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i]
        const b = particles[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const dist = Math.hypot(dx, dy)
        if (dist < LINK_DIST) {
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.strokeStyle = `rgba(100, 160, 255, ${0.28 * (1 - dist / LINK_DIST)})`
          ctx.lineWidth = 1
          ctx.stroke()
        }
      }
    }
    rafId = requestAnimationFrame(render)
  }
  render()
}

onMounted(() => drawParticles())
onBeforeUnmount(() => cancelAnimationFrame(rafId))

// 演示用打字机标语
const slogans = ['一句话，生成完整应用', '零代码，释放你的创造力', 'AI 驱动的应用工厂']
const typedText = ref('')
let sloganIdx = 0
let charIdx = 0
let deleting = false
let typeTimer = 0

const typeLoop = () => {
  const current = slogans[sloganIdx]
  typedText.value = current.slice(0, charIdx)
  if (!deleting && charIdx < current.length) {
    charIdx++
  } else if (!deleting) {
    deleting = true
    typeTimer = window.setTimeout(typeLoop, 1600)
    return
  } else if (charIdx > 0) {
    charIdx--
  } else {
    deleting = false
    sloganIdx = (sloganIdx + 1) % slogans.length
  }
  typeTimer = window.setTimeout(typeLoop, deleting ? 45 : 95)
}
onMounted(() => typeLoop())
onBeforeUnmount(() => clearTimeout(typeTimer))
</script>

<template>
  <div class="login-page">
    <!-- 动态粒子背景 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    <!-- 霓虹光斑 -->
    <div class="glow glow--cyan"></div>
    <div class="glow glow--purple"></div>
    <!-- 网格地平线 -->
    <div class="grid-floor"></div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-panel">
        <div class="brand-badge">
          <span class="brand-badge__icon">AI</span>
          <span class="brand-badge__text">Hu AI 零代码应用生成平台</span>
        </div>
        <h1 class="brand-title">
          让 <em>AI</em> 为你<br />
          编写整个世界
        </h1>
        <div class="brand-slogan">
          <span class="brand-slogan__cursor">{{ typedText }}<i class="cursor">|</i></span>
        </div>
        <ul class="brand-features">
          <li>⚡ 秒级生成 · 从想法到上线</li>
          <li>🧩 模块自由拼装 · 零门槛</li>
          <li>🔒 企业级安全 · 私有化部署</li>
        </ul>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="card-panel">
        <div class="login-card">
          <h2 class="login-card__title">欢迎回来</h2>
          <p class="login-card__subtitle">登录你的账号，继续创造</p>

          <a-form :model="formState" layout="vertical" autocomplete="off" @finish="onFinish">
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                v-model:value="formState.userAccount"
                placeholder="账号"
                size="large"
                autocomplete="off"
                allow-clear
              />
            </a-form-item>
            <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password
                v-model:value="formState.userPassword"
                placeholder="密码"
                size="large"
                autocomplete="new-password"
              />
            </a-form-item>
            <a-form-item class="login-card__submit">
              <button class="neon-btn" type="submit" :disabled="submitting">
                <span v-if="!submitting">登 录</span>
                <span v-else>登录中…</span>
              </button>
            </a-form-item>
          </a-form>

          <button class="ghost-btn" type="button" @click="router.push('/user/register')">
            没有账号？去注册
          </button>

          <div class="login-card__footer">
            已经有账号？
            <router-link to="/user/login" class="register-link">直接登录</router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="page-footer">© 2026 Hu AI · Powered by AI Code Mother</div>
  </div>
</template>

<style scoped>
/* ───────── 页面容器与背景 ───────── */
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 30% 20%, #0d1b3e 0%, #070d21 55%, #04060f 100%);
  color: #e8f0ff;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  opacity: 0.9;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  z-index: 0;
  animation: glow-float 9s ease-in-out infinite alternate;
}

.glow--cyan {
  width: 420px;
  height: 420px;
  left: -120px;
  top: -100px;
  background: rgba(0, 190, 255, 0.22);
}

.glow--purple {
  width: 480px;
  height: 480px;
  right: -140px;
  bottom: -160px;
  background: rgba(130, 60, 255, 0.22);
  animation-delay: -4.5s;
}

@keyframes glow-float {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(50px, 35px) scale(1.15);
  }
}

/* 网格地平线 */
.grid-floor {
  position: absolute;
  left: -25%;
  right: -25%;
  bottom: -10%;
  height: 46%;
  z-index: 0;
  background-image:
    linear-gradient(rgba(60, 140, 255, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(60, 140, 255, 0.16) 1px, transparent 1px);
  background-size: 44px 44px;
  transform: perspective(420px) rotateX(62deg);
  mask-image: linear-gradient(to top, rgba(0, 0, 0, 0.85), transparent);
  -webkit-mask-image: linear-gradient(to top, rgba(0, 0, 0, 0.85), transparent);
}

/* ───────── 主内容区 ───────── */
.login-container {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 72px;
  padding: 48px 32px;
}

/* ── 左侧品牌 ── */
.brand-panel {
  max-width: 480px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 7px 16px;
  border: 1px solid rgba(80, 180, 255, 0.35);
  border-radius: 999px;
  background: rgba(20, 60, 140, 0.25);
  backdrop-filter: blur(6px);
  font-size: 13px;
  letter-spacing: 0.5px;
  color: #9fd0ff;
}

.brand-badge__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #00c6ff, #7b2fff);
  box-shadow: 0 0 12px rgba(0, 198, 255, 0.6);
}

.brand-title {
  margin: 26px 0 18px;
  font-size: 44px;
  line-height: 1.25;
  font-weight: 800;
  color: #f2f7ff;
  text-shadow: 0 0 30px rgba(80, 160, 255, 0.35);
}

.brand-title em {
  font-style: normal;
  background: linear-gradient(90deg, #00e0ff, #8a5cff, #ff5ce1);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: hue-slide 4s linear infinite;
}

@keyframes hue-slide {
  to {
    background-position: 200% center;
  }
}

.brand-slogan {
  min-height: 30px;
  font-size: 17px;
  color: #7ea8d8;
  font-family: 'Consolas', 'Courier New', monospace;
}

.cursor {
  font-style: normal;
  color: #00e0ff;
  animation: blink 0.9s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.brand-features {
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 14.5px;
  color: #a9c3e8;
}

.brand-features li {
  padding-left: 4px;
  border-left: 2px solid transparent;
  transition: all 0.3s;
}

.brand-features li:hover {
  border-left-color: #00e0ff;
  color: #e8f0ff;
  transform: translateX(4px);
}

/* ── 右侧玻璃卡片 ── */
.card-panel {
  perspective: 1200px;
}

.login-card {
  width: 400px;
  padding: 40px 36px 32px;
  border-radius: 20px;
  border: 1px solid rgba(110, 170, 255, 0.22);
  background: rgba(13, 24, 52, 0.62);
  backdrop-filter: blur(18px);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  animation: card-in 0.7s cubic-bezier(0.2, 0.8, 0.25, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(26px) rotateX(6deg);
  }
  to {
    opacity: 1;
    transform: translateY(0) rotateX(0);
  }
}

.login-card__title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #f2f7ff;
}

.login-card__subtitle {
  margin: 8px 0 28px;
  font-size: 14px;
  color: #7ea8d8;
}

/* 输入框暗色化 */
.login-card :deep(.ant-input-affix-wrapper),
.login-card :deep(.ant-input) {
  background: rgba(8, 16, 38, 0.7) !important;
  border-color: rgba(90, 140, 220, 0.35) !important;
  color: #e8f0ff !important;
}

.login-card :deep(.ant-input::placeholder),
.login-card :deep(.ant-input-affix-wrapper .ant-input::placeholder) {
  color: #54749e !important;
}

.login-card :deep(.ant-input-affix-wrapper:hover),
.login-card :deep(.ant-input-affix-wrapper-focused) {
  border-color: #00c6ff !important;
  box-shadow: 0 0 0 2px rgba(0, 198, 255, 0.18) !important;
}

.login-card :deep(.ant-input-password-icon) {
  color: #7ea8d8 !important;
}

.login-card :deep(.ant-form-item-with-help .ant-form-item-explain-error) {
  color: #ff7a9c;
}

/* 霓虹登录按钮 */
.neon-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 6px;
  color: #fff;
  background: linear-gradient(90deg, #0062ff, #00c6ff, #7b2fff);
  background-size: 200% auto;
  box-shadow:
    0 0 18px rgba(0, 170, 255, 0.55),
    0 6px 20px rgba(0, 60, 200, 0.4);
  transition: all 0.35s ease;
  animation: hue-slide 5s linear infinite;
}

.neon-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 0 32px rgba(0, 190, 255, 0.85),
    0 10px 28px rgba(0, 80, 255, 0.5);
}

.neon-btn:active:not(:disabled) {
  transform: translateY(0);
}

.neon-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.login-card__submit {
  margin-bottom: 10px;
}

/* 幽灵注册按钮 */
.ghost-btn {
  width: 100%;
  height: 46px;
  margin-bottom: 18px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #9fd0ff;
  background: transparent;
  border: 1px solid rgba(90, 160, 255, 0.45);
  transition: all 0.3s ease;
}

.ghost-btn:hover {
  color: #e8f0ff;
  border-color: #00c6ff;
  background: rgba(0, 198, 255, 0.1);
  box-shadow: 0 0 16px rgba(0, 198, 255, 0.35);
  transform: translateY(-2px);
}

.ghost-btn:active {
  transform: translateY(0);
}

.login-card__footer {
  text-align: center;
  font-size: 13.5px;
  color: #7ea8d8;
}

.register-link {
  color: #00c6ff;
  font-weight: 600;
  transition: all 0.3s;
}

.register-link:hover {
  color: #7bd9ff;
  text-shadow: 0 0 10px rgba(0, 198, 255, 0.6);
}

/* ───────── 页脚 ───────── */
.page-footer {
  position: relative;
  z-index: 2;
  margin-top: 40px;
  font-size: 12.5px;
  color: #4c6791;
  letter-spacing: 1px;
}

/* ───────── 响应式 ───────── */
@media (max-width: 960px) {
  .login-container {
    flex-direction: column;
    gap: 36px;
    text-align: center;
  }

  .brand-panel {
    max-width: 90vw;
  }

  .brand-title {
    font-size: 32px;
  }

  .brand-features {
    display: none;
  }

  .login-card {
    width: min(400px, 90vw);
  }
}
</style>
