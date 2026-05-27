<template>
  <div class="ai-layout">
    <!-- Sidebar -->
    <aside class="ai-sidebar">
      <div class="sidebar-header">
        <a href="/home" class="back-btn" title="返回首页">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
        </a>
        <span class="sidebar-title">AI助手</span>
      </div>

      <button class="new-chat-btn" @click="startNewChat">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        新对话
      </button>

      <div class="history-list-container">
        <div class="history-list-title">历史对话</div>
        <div id="sessionList">
          <div 
            v-for="(session, index) in chatSessions" 
            :key="session.id"
            class="history-item"
            :class="{ active: currentSessionId === session.id }"
            @click="switchSession(session.id)"
          >
            <span class="history-item-text">{{ session.title || '新对话' }}</span>
            <button class="history-item-delete" @click.stop="deleteSession(session.id)">✕</button>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="user-profile">
          <span class="nav-avatar">👤</span>
          <span>用户</span>
        </div>
        <button @click="toggleTheme" class="theme-switch" aria-label="切换主题">
          <span class="theme-icon">{{ isDarkTheme ? '🌙' : '☀️' }}</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <div class="ai-container">
        <div class="chat-header">
          <div class="chat-title">{{ currentSession?.title || '新对话' }}</div>
        </div>

        <div class="chat-history" ref="chatHistory">
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            class="chat-message"
            :class="{ 'message-user': message.isUser, 'message-ai': !message.isUser }"
          >
            <div class="message-avatar">
              {{ message.isUser ? '👤' : '🤖' }}
            </div>
            <div class="message-content">
              <button class="copy-btn" @click="copyMessage(message.content)">📋</button>
              <div v-if="message.isUser">{{ message.content }}</div>
              <div v-else v-html="formatMessage(message.content)" class="markdown-body"></div>
              <div v-if="message.isTyping" class="typing-indicator">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="prompt-suggestions">
            <div class="prompt-chip" v-for="(prompt, index) in promptSuggestions" :key="index" @click="fillPrompt(prompt)">
              {{ prompt }}
            </div>
          </div>

          <div class="chat-input-wrapper">
            <textarea 
              class="chat-input" 
              v-model="inputMessage"
              placeholder="输入你的问题，按 Enter 发送，Shift + Enter 换行..." 
              rows="1"
              @keydown="handleKeydown"
            ></textarea>
            <div class="input-actions">
              <button class="stop-btn" :class="{ visible: isLoading }" @click="stopGeneration">⏹</button>
              <button class="send-btn" :disabled="!inputMessage.trim() || isLoading" @click="sendMessage">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13"></line>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                </svg>
              </button>
            </div>
          </div>
          <div class="disclaimer">
            AI 可能产生不准确的信息，请核实重要内容。
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

// DeepSeek API 配置
const API_KEY = 'sk-0ab12c9e84ae4d75b5b78fc6a4f2edab'
const API_URL = 'https://api.deepseek.com/v1/chat/completions'

// 响应式数据
const isDarkTheme = ref(false)
const inputMessage = ref('')
const isLoading = ref(false)
const messages = ref([])
const chatSessions = ref([])
const currentSessionId = ref(null)
const chatHistory = ref(null)

// 提示建议
const promptSuggestions = [
  '🔍 解释冒泡排序原理',
  '🌲 二叉树的前中后序遍历有什么区别？',
  '💻 用 Java 实现快速排序',
  '⏱️ 常见排序算法的时间复杂度总结',
  '🛤️ Dijkstra 算法是如何寻找最短路径的？',
  '📚 Java 中 HashMap 和 ConcurrentHashMap 的区别',
  '🌐 HTTP 和 HTTPS 的区别是什么？',
  '💾 数据库索引的作用和原理'
]

// 当前会话
const currentSession = computed(() => {
  return chatSessions.value.find(s => s.id === currentSessionId.value)
})

// 格式化消息（支持 Markdown）
const formatMessage = (content) => {
  try {
    const html = marked(content)
    return html.replace(/<pre>/g, '<pre><code class="hljs">').replace(/<\/pre>/g, '</code></pre>')
  } catch {
    return content
  }
}

// 切换主题
const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('ai-theme', isDarkTheme.value ? 'dark' : 'light')
  document.documentElement.setAttribute('data-theme', isDarkTheme.value ? 'dark' : 'light')
}

// 填充提示
const fillPrompt = (prompt) => {
  inputMessage.value = prompt.replace(/[🔍🌲💻⏱️🛤️📚🌐💾]/g, '').trim()
}

// 复制消息
const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    alert('已复制到剪贴板')
  } catch (err) {
    console.error('复制失败:', err)
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (chatHistory.value) {
    chatHistory.value.scrollTop = chatHistory.value.scrollHeight
  }
}

// 发送消息（流式）
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    content,
    isUser: true,
    isTyping: false
  })
  inputMessage.value = ''
  await scrollToBottom()

  // 更新会话标题
  if (!currentSession.value?.title) {
    updateSessionTitle(content)
  }

  // 添加 AI 正在回复的状态（流式更新）
  const typingMessage = {
    id: Date.now() + 1,
    content: '',
    isUser: false,
    isTyping: true
  }
  messages.value.push(typingMessage)
  await scrollToBottom()

  isLoading.value = true
  let fullContent = ''

  try {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`
      },
      body: JSON.stringify({
        model: 'deepseek-chat',
        messages: messages.value.slice(0, -1).map(m => ({
          role: m.isUser ? 'user' : 'assistant',
          content: m.content
        })),
        max_tokens: 2048,
        temperature: 0.7,
        stream: true
      })
    })

    if (!response.ok) {
      throw new Error(`API 请求失败: ${response.status}`)
    }

    // 处理流式响应
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let done = false

    while (!done) {
      const { value, done: readerDone } = await reader.read()
      done = readerDone
      if (value) {
        const chunk = decoder.decode(value, { stream: !done })
        // 解析 SSE 格式数据
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (data === '[DONE]') {
              done = true
              break
            }
            try {
              const json = JSON.parse(data)
              const delta = json.choices?.[0]?.delta?.content || ''
              if (delta) {
                fullContent += delta
                // 更新消息内容
                const typingIndex = messages.value.findIndex(m => m.id === typingMessage.id)
                if (typingIndex !== -1) {
                  messages.value[typingIndex].content = fullContent
                  messages.value[typingIndex].isTyping = false
                }
                await scrollToBottom()
              }
            } catch (e) {
              // 忽略解析错误
            }
          }
        }
      }
    }

    // 确保最终内容完整
    const typingIndex = messages.value.findIndex(m => m.id === typingMessage.id)
    if (typingIndex !== -1) {
      messages.value[typingIndex].content = fullContent || '暂无回答'
      messages.value[typingIndex].isTyping = false
    }

    // 保存会话
    saveSession()
  } catch (error) {
    console.error('API 请求错误:', error)
    const typingIndex = messages.value.findIndex(m => m.id === typingMessage.id)
    if (typingIndex !== -1) {
      messages.value[typingIndex] = {
        id: typingMessage.id,
        content: '抱歉，暂时无法回答您的问题，请稍后重试。',
        isUser: false,
        isTyping: false
      }
    }
  } finally {
    isLoading.value = false
    await scrollToBottom()
    // 高亮代码
    highlightCode()
  }
}

// 停止生成
const stopGeneration = () => {
  // 可以在这里添加停止请求的逻辑
  isLoading.value = false
}

// 键盘事件
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 创建新会话
const startNewChat = () => {
  const newSession = {
    id: Date.now().toString(),
    title: null,
    messages: []
  }
  chatSessions.value.unshift(newSession)
  currentSessionId.value = newSession.id
  messages.value = []
}

// 切换会话
const switchSession = (sessionId) => {
  currentSessionId.value = sessionId
  const session = chatSessions.value.find(s => s.id === sessionId)
  if (session) {
    messages.value = JSON.parse(JSON.stringify(session.messages))
  }
}

// 删除会话
const deleteSession = (sessionId) => {
  const index = chatSessions.value.findIndex(s => s.id === sessionId)
  if (index !== -1) {
    chatSessions.value.splice(index, 1)
    if (currentSessionId.value === sessionId) {
      if (chatSessions.value.length > 0) {
        currentSessionId.value = chatSessions.value[0].id
        messages.value = JSON.parse(JSON.stringify(chatSessions.value[0].messages))
      } else {
        startNewChat()
      }
    }
  }
  saveSessionsToStorage()
}

// 更新会话标题
const updateSessionTitle = (content) => {
  const session = chatSessions.value.find(s => s.id === currentSessionId.value)
  if (session) {
    session.title = content.substring(0, 30) + (content.length > 30 ? '...' : '')
    saveSessionsToStorage()
  }
}

// 保存会话
const saveSession = () => {
  const session = chatSessions.value.find(s => s.id === currentSessionId.value)
  if (session) {
    session.messages = JSON.parse(JSON.stringify(messages.value))
    saveSessionsToStorage()
  }
}

// 保存到本地存储
const saveSessionsToStorage = () => {
  localStorage.setItem('ai-chat-sessions', JSON.stringify(chatSessions.value))
}

// 从本地存储加载
const loadSessionsFromStorage = () => {
  const saved = localStorage.getItem('ai-chat-sessions')
  if (saved) {
    try {
      chatSessions.value = JSON.parse(saved)
      if (chatSessions.value.length > 0) {
        currentSessionId.value = chatSessions.value[0].id
        messages.value = JSON.parse(JSON.stringify(chatSessions.value[0].messages))
      } else {
        startNewChat()
      }
    } catch {
      startNewChat()
    }
  } else {
    startNewChat()
  }
}

// 高亮代码
const highlightCode = () => {
  setTimeout(() => {
    document.querySelectorAll('pre code').forEach(block => {
      hljs.highlightElement(block)
    })
  }, 100)
}

// 初始化
onMounted(() => {
  // 加载主题
  const savedTheme = localStorage.getItem('ai-theme') || 'light'
  isDarkTheme.value = savedTheme === 'dark'
  document.documentElement.setAttribute('data-theme', savedTheme)

  // 加载会话
  loadSessionsFromStorage()

  // 初始化代码高亮
  hljs.initHighlightingOnLoad()
})

// 监听消息变化，自动滚动
watch(messages, async () => {
  await scrollToBottom()
}, { deep: true })
</script>

<style>
:root {
  --bg-dark: #0d1117;
  --bg-card: #161b22;
  --bg-card-hover: #21262d;
  --border: #30363d;
  --text-primary: #e6edf3;
  --text-secondary: #8b949e;
  --text-muted: #6e7681;
  --primary: #58a6ff;
  --primary-dark: #1f6feb;
  --primary-light: #79c0ff;
  --secondary: #8957e5;
  --accent: #f0883e;
  --danger: #f85149;
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
}

[data-theme="light"] {
  --bg-dark: #ffffff;
  --bg-card: #f6f8fa;
  --bg-card-hover: #f1f3f4;
  --border: #d0d7de;
  --text-primary: #21262d;
  --text-secondary: #586069;
  --text-muted: #8b949e;
  --primary: #0969da;
  --primary-dark: #0550ae;
  --primary-light: #388bfd;
}

/* AI Page Specific Layout */
.ai-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--bg-dark);
  overflow: hidden;
}

/* Sidebar Styles */
.ai-sidebar {
  width: 260px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  z-index: 10;
}

.sidebar-header {
  padding: var(--spacing-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border);
}

.sidebar-title {
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--text-primary);
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(102, 126, 234, 0.1);
  color: var(--primary);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: var(--radius-md);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: rgba(102, 126, 234, 0.2);
  transform: translateY(-1px);
}

.history-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 var(--spacing-sm);
}

.history-list-title {
  font-size: 0.8rem;
  color: var(--text-muted);
  padding: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.history-item:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.history-item.active {
  background: rgba(102, 126, 234, 0.1);
  color: var(--primary);
  font-weight: 500;
}

.history-item-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item-delete {
  opacity: 0;
  color: var(--text-muted);
  border: none;
  background: none;
  cursor: pointer;
  padding: 2px;
  font-size: 0.8rem;
}

.history-item:hover .history-item-delete {
  opacity: 1;
}

.history-item-delete:hover {
  color: var(--danger);
}

.sidebar-footer {
  padding: var(--spacing-md);
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 500;
}

.nav-avatar {
  font-size: 1rem;
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background: var(--primary);
  color: white;
}

.theme-switch {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: background 0.2s;
}

.theme-switch:hover {
  background: var(--bg-card-hover);
}

.theme-icon {
  font-size: 1.2rem;
}

/* Main Content Styles */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.ai-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: var(--spacing-lg) var(--spacing-lg) 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border);
  margin-bottom: var(--spacing-md);
}

.chat-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding-right: var(--spacing-sm);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.chat-message {
  display: flex;
  gap: var(--spacing-md);
  max-width: 95%;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-ai {
  align-self: flex-start;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.message-user .message-avatar {
  background: var(--primary);
  color: white;
}

.message-ai .message-avatar {
  background: linear-gradient(135deg, var(--secondary), var(--accent));
  color: white;
}

.message-content {
  background: var(--bg-card);
  border: 1px solid var(--border);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  line-height: 1.6;
  position: relative;
  overflow: hidden;
  max-width: calc(100% - 44px);
}

.message-user .message-content {
  background: var(--primary-dark);
  color: white;
  border-color: var(--primary-dark);
  border-top-right-radius: 0;
}

.message-ai .message-content {
  border-top-left-radius: 0;
}

.copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255,255,255,0.1);
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  font-size: 0.8rem;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-user .copy-btn {
  color: rgba(255,255,255,0.8);
}

.message-content:hover .copy-btn {
  opacity: 1;
}

.copy-btn:hover {
  color: var(--text-primary);
  background: rgba(255,255,255,0.2);
}

/* Markdown Styles */
.markdown-body {
  font-size: 0.95rem;
}

.markdown-body p {
  margin-bottom: 0.5rem;
}

.markdown-body p:last-child {
  margin-bottom: 0;
}

.markdown-body pre {
  background: #1e1e2e;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1rem 0;
  border: 1px solid #2d2d4a;
}

.markdown-body pre code {
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 0.9em;
  color: #abb2bf;
  background: transparent;
  padding: 0;
  border-radius: 0;
}

.markdown-body p code {
  background: rgba(102, 126, 234, 0.2);
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  color: var(--primary-light);
  font-family: 'Fira Code', monospace;
}

.markdown-body ul, .markdown-body ol {
  padding-left: 2rem;
  margin-bottom: 1rem;
}

.markdown-body li {
  margin-bottom: 0.25rem;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 1rem;
}

.markdown-body th, .markdown-body td {
  border: 1px solid var(--border);
  padding: 8px 12px;
  text-align: left;
}

.markdown-body th {
  background: var(--bg-card-hover);
  font-weight: 600;
}

.markdown-body h1, .markdown-body h2, .markdown-body h3 {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.markdown-body h1 { font-size: 1.5rem; }
.markdown-body h2 { font-size: 1.3rem; }
.markdown-body h3 { font-size: 1.1rem; }

/* Chat Input Area */
.chat-input-area {
  padding: var(--spacing-sm) 0 var(--spacing-lg);
  background: var(--bg-dark);
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prompt-suggestions {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}

.prompt-suggestions::-webkit-scrollbar {
  display: none;
}

.prompt-chip {
  white-space: nowrap;
  padding: 6px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  font-size: 0.85rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.prompt-chip:hover {
  background: rgba(102, 126, 234, 0.1);
  color: var(--primary);
  border-color: rgba(102, 126, 234, 0.3);
}

.chat-input-wrapper {
  position: relative;
  display: flex;
  align-items: flex-end;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xs) var(--spacing-sm);
  transition: border-color 0.2s;
}

.chat-input-wrapper:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.chat-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 1rem;
  padding: var(--spacing-sm);
  resize: none;
  min-height: 44px;
  max-height: 200px;
  font-family: inherit;
  line-height: 1.5;
  outline: none;
}

.chat-input::placeholder {
  color: var(--text-muted);
}

.input-actions {
  display: flex;
  gap: 8px;
  padding: var(--spacing-sm);
}

.send-btn, .stop-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn {
  background: var(--primary);
  color: white;
}

.send-btn:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: scale(1.05);
}

.send-btn:disabled {
  background: var(--border);
  color: var(--text-muted);
  cursor: not-allowed;
}

.stop-btn {
  background: var(--bg-card-hover);
  color: var(--danger);
  border: 1px solid var(--border);
  display: none;
}

.stop-btn:hover {
  background: var(--border);
  color: #ff5555;
}

.stop-btn.visible {
  display: flex;
}

/* Loading Indicator */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.typing-dot {
  width: 6px;
  height: 6px;
  background: var(--text-secondary);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.disclaimer {
  text-align: center;
  color: var(--text-muted);
  font-size: 0.75rem;
  margin-top: 8px;
}

/* 浅色主题代码块适配 */
[data-theme="light"] .markdown-body pre {
  background: #282c34;
  border: 1px solid #e2e8f0;
}

[data-theme="light"] .markdown-body p code {
  background: rgba(9, 105, 218, 0.1);
  color: #0969da;
}
</style>