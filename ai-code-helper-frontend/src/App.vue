<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { streamChat } from './services/chat'

const STORAGE_KEY = 'ai-code-helper-memory-id'
const messages = ref([])
const input = ref('')
const isStreaming = ref(false)
const isComposing = ref(false)
const isCopied = ref(false)
const errorMessage = ref('')
const messagesContainer = ref(null)
const abortController = ref(null)
const memoryId = ref(null)

const quickPrompts = [
  { icon: '</>', title: '解释一段代码', text: '请帮我解释这段代码的执行逻辑' },
  { icon: '⌁', title: '准备面试', text: '帮我整理一份 Java 面试高频题清单' },
  { icon: '✦', title: '学习规划', text: '我想系统学习前端，请给我一份路线图' },
]

const canSend = computed(() => input.value.trim().length > 0 && !isStreaming.value)
const statusLabel = computed(() => (isStreaming.value ? '正在思考' : '在线'))

function createMemoryId(forceNew = false) {
  const saved = Number(sessionStorage.getItem(STORAGE_KEY))

  if (!forceNew && Number.isInteger(saved) && saved > 0) {
    memoryId.value = saved
  } else {
    let newId = Date.now() % 2147483647

    if (newId === memoryId.value) {
      newId += 1
    }

    memoryId.value = newId
  }

  sessionStorage.setItem(STORAGE_KEY, String(memoryId.value))
}

function formatTime(date = new Date()) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  })
}

function renderText(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

async function sendMessage(text = input.value) {
  const content = text.trim()
  if (!content || isStreaming.value) return

  input.value = ''
  errorMessage.value = ''
  messages.value.push({ role: 'user', content, time: formatTime() })
  const aiMessage = { role: 'assistant', content: '', time: formatTime(), pending: true }
  messages.value.push(aiMessage)
  isStreaming.value = true
  abortController.value = new AbortController()
  scrollToBottom()

  try {
    await streamChat({
      memoryId: memoryId.value,
      message: content,
      signal: abortController.value.signal,
      onChunk: async (chunk) => {
        aiMessage.content += chunk
        aiMessage.pending = false
        scrollToBottom()
        await nextTick()
      },
    })
    if (!aiMessage.content) aiMessage.content = '我暂时没有收到有效的回答，请稍后再试。'
  } catch (error) {
    if (error.name !== 'AbortError') {
      aiMessage.content = ''
      errorMessage.value = '连接服务失败，请确认后端已运行在 localhost:8081。'
      messages.value.pop()
    }
  } finally {
    aiMessage.pending = false
    isStreaming.value = false
    abortController.value = null
    scrollToBottom()
  }
}

function stopStreaming() {
  abortController.value?.abort()
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !isComposing.value) {
    event.preventDefault()
    sendMessage()
  }
}

async function copyMessage(content) {
  await navigator.clipboard.writeText(content)
  isCopied.value = true
  window.setTimeout(() => { isCopied.value = false }, 1600)
}

function newConversation() {
  createMemoryId(true)
  messages.value = []
  errorMessage.value = ''
}

onMounted(() => createMemoryId())
onUnmounted(() => abortController.value?.abort())
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">⌘</div>
        <div>
          <div class="brand-name">AI 编程小助手</div>
          <div class="brand-caption">CODE / LEARN / GROW</div>
        </div>
      </div>

      <div class="sidebar-content">
        <button class="new-chat-button" type="button" @click="newConversation">
          <span class="plus-icon">+</span>
          <span>开启新对话</span>
          <span class="shortcut">⌘ K</span>
        </button>

        <div class="nav-label">当前会话</div>
        <div class="current-session">
          <span class="session-dot"></span>
          <span class="session-copy">
            <strong>编程学习助手</strong>
            <small>刚刚开始</small>
          </span>
          <span class="more-icon">···</span>
        </div>

        <div class="nav-label prompt-label">你可以这样问</div>
        <button
          v-for="prompt in quickPrompts"
          :key="prompt.title"
          class="prompt-item"
          type="button"
          @click="sendMessage(prompt.text)"
        >
          <span class="prompt-icon">{{ prompt.icon }}</span>
          <span>
            <strong>{{ prompt.title }}</strong>
            <small>{{ prompt.text }}</small>
          </span>
        </button>
      </div>

      <div class="sidebar-footer">
        <div class="model-status">
          <span class="status-pulse"></span>
          <span><strong>AI 助手</strong><small>服务正常运行</small></span>
          <span class="chevron">⌄</span>
        </div>
        <div class="footer-links"><span>帮助中心</span><span>·</span><span>反馈</span></div>
      </div>
    </aside>

    <main class="chat-panel">
      <header class="chat-header">
        <div class="header-title">
          <div class="assistant-avatar">✦</div>
          <div>
            <h1>AI 编程小助手</h1>
            <div class="online-state"><span class="online-dot"></span>{{ statusLabel }} · 随时为你解答</div>
          </div>
        </div>
        <div class="header-actions">
          <div class="memory-id" title="当前会话标识">会话 ID <strong>#{{ memoryId }}</strong></div>
          <button class="icon-button" type="button" title="更多选项" aria-label="更多选项">···</button>
        </div>
      </header>

      <section ref="messagesContainer" class="messages-area" aria-live="polite">
        <div v-if="messages.length === 0" class="welcome-state">
          <div class="welcome-orbit"><span>✦</span></div>
          <p class="eyebrow">YOUR CODING COMPANION</p>
          <h2>你好，我是你的<br><em>编程小助手</em></h2>
          <p class="welcome-copy">无论是代码调试、知识梳理，还是面试准备，<br>把问题交给我，我们一起找到清晰的答案。</p>
          <div class="welcome-line"><span></span><b>从一个问题开始</b><span></span></div>
        </div>

        <div v-for="(message, index) in messages" :key="`${message.time}-${index}`" :class="['message-row', message.role]">
          <div v-if="message.role === 'assistant'" class="message-avatar">✦</div>
          <div class="message-block">
            <div class="message-meta">
              <strong>{{ message.role === 'assistant' ? 'AI 编程小助手' : '我' }}</strong>
              <span>{{ message.time }}</span>
            </div>
            <div class="message-bubble" :class="{ 'is-pending': message.pending }">
              <span v-if="message.pending && !message.content" class="typing-indicator"><i></i><i></i><i></i></span>
              <span v-else v-html="renderText(message.content)"></span>
            </div>
            <div v-if="message.role === 'assistant' && message.content && !message.pending" class="message-tools">
              <button type="button" title="复制回答" @click="copyMessage(message.content)">{{ isCopied ? '已复制' : '复制' }}</button>
            </div>
          </div>
          <div v-if="message.role === 'user'" class="user-avatar">我</div>
        </div>

        <div v-if="errorMessage" class="error-note">{{ errorMessage }}</div>
      </section>

      <footer class="composer-wrap">
        <div class="composer">
          <textarea
            v-model="input"
            rows="1"
            placeholder="输入你的编程问题..."
            :disabled="isStreaming"
            @keydown="handleKeydown"
            @compositionstart="isComposing = true"
            @compositionend="isComposing = false"
          ></textarea>
          <div class="composer-bottom">
            <div class="composer-hint"><span class="sparkle">✧</span> AI 会认真思考你的每一个问题</div>
            <div class="composer-actions">
              <span class="enter-hint">Enter 发送 · Shift + Enter 换行</span>
              <button v-if="isStreaming" class="send-button stop" type="button" title="停止生成" aria-label="停止生成" @click="stopStreaming"><span></span></button>
              <button v-else class="send-button" type="button" title="发送问题" aria-label="发送问题" :disabled="!canSend" @click="sendMessage"><span>↑</span></button>
            </div>
          </div>
        </div>
        <p class="disclaimer">AI 生成的内容仅供参考，请结合实际情况判断。</p>
      </footer>
    </main>
  </div>
</template>
