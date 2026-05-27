<template>
  <div class="question-study-page" :class="{ 'dark-mode': isDarkMode, 'fullscreen': isFullscreen }">
    <!-- 顶部导航栏 -->
    <div class="top-nav-bar">
      <div class="nav-left">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>题目学习</el-breadcrumb-item>
          <el-breadcrumb-item>
            <el-select v-model="selectedCategory" placeholder="选择题库分类" @change="loadQuestions" style="width: 180px">
              <el-option 
                v-for="cat in categories" 
                :key="cat.id" 
                :label="cat.name" 
                :value="cat.id" 
              />
            </el-select>
          </el-breadcrumb-item>
        </el-breadcrumb>
        
        <div class="question-progress">
          <span class="progress-text">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
          <el-progress :percentage="progressPercent" :stroke-width="8" style="width: 200px; margin-left: 15px" />
        </div>
        
        <el-tag :type="getDifficultyType(currentQuestion?.difficulty)" class="difficulty-tag">
          {{ getDifficultyText(currentQuestion?.difficulty) }}
        </el-tag>
      </div>
      
      <div class="nav-right">
        <el-button :type="isFavorite ? 'warning' : 'default'" @click="toggleFavorite" :icon="isFavorite ? 'Star' : 'StarFilled'">
          {{ isFavorite ? '已收藏' : '收藏' }}
        </el-button>
        <el-button :type="isWrongAnswer ? 'danger' : 'default'" @click="toggleWrongMark" :icon="CircleClose">
          {{ isWrongAnswer ? '错题' : '标记错题' }}
        </el-button>
        <el-button @click="toggleFullscreen" :icon="FullScreen">全屏</el-button>
        <el-button @click="toggleDarkMode" :icon="Moon">
          {{ isDarkMode ? '白昼' : '黑夜' }}
        </el-button>
        <el-button @click="goBack" :icon="Back">返回</el-button>
      </div>
    </div>
    
    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 左侧题目展示区 -->
      <div class="left-section">
        <!-- 倒计时器（可选） -->
        <div v-if="isTimerMode" class="timer-bar">
          <el-icon><Timer /></el-icon>
          <span class="timer-text">{{ formatTime(timerSeconds) }}</span>
        </div>
        
        <!-- 题目展示区 -->
        <div class="question-display">
          <div class="question-title" :style="{ fontSize: fontSize + 'px' }">
            {{ currentQuestion?.title }}
          </div>
          
          <!-- 代码高亮区域（如果是代码题） -->
          <div v-if="currentQuestion?.code" class="code-block">
            <pre><code>{{ currentQuestion.code }}</code></pre>
          </div>
          
          <!-- 题目配图 -->
          <div v-if="currentQuestion?.imageUrl" class="question-image">
            <el-image :src="currentQuestion.imageUrl" fit="contain" />
          </div>
          
          <!-- 答题区域 -->
          <div class="answer-area">
            <!-- 选择题 -->
            <template v-if="currentQuestion?.type === 'CHOICE'">
              <el-radio-group v-if="!currentQuestion?.multi" v-model="userAnswer" @change="answerChanged">
                <div 
                  v-for="(opt, idx) in currentChoices" 
                  :key="idx" 
                  class="choice-option"
                  :class="{ 
                    'selected': userAnswer === idx,
                    'correct': showAnswer && opt.isCorrect,
                    'wrong': showAnswer && userAnswer === idx && !opt.isCorrect
                  }"
                  @click="selectChoice(idx)"
                >
                  <span class="choice-label">{{ String.fromCharCode(65 + idx) }}.</span>
                  <span class="choice-content">{{ opt.content }}</span>
                </div>
              </el-radio-group>
              
              <el-checkbox-group v-else v-model="userMultiAnswer" @change="answerChanged">
                <div 
                  v-for="(opt, idx) in currentChoices" 
                  :key="idx" 
                  class="choice-option"
                  :class="{ 
                    'selected': userMultiAnswer.includes(idx),
                    'correct': showAnswer && opt.isCorrect,
                    'wrong': showAnswer && userMultiAnswer.includes(idx) && !opt.isCorrect
                  }"
                  @click="toggleMultiChoice(idx)"
                >
                  <span class="choice-label">{{ String.fromCharCode(65 + idx) }}.</span>
                  <span class="choice-content">{{ opt.content }}</span>
                </div>
              </el-checkbox-group>
            </template>
            
            <!-- 判断题 -->
            <template v-else-if="currentQuestion?.type === 'JUDGE'">
              <div class="judge-options">
                <el-button 
                  size="large"
                  :type="userAnswer === 'TRUE' ? 'success' : 'default'"
                  @click="userAnswer = 'TRUE'; answerChanged()"
                >
                  正确
                </el-button>
                <el-button 
                  size="large"
                  :type="userAnswer === 'FALSE' ? 'danger' : 'default'"
                  @click="userAnswer = 'FALSE'; answerChanged()"
                >
                  错误
                </el-button>
              </div>
            </template>
            
            <!-- 填空题 -->
            <template v-else-if="currentQuestion?.type === 'FILL'">
              <el-input 
                v-model="userAnswer" 
                type="textarea" 
                :rows="4"
                placeholder="请填写答案"
                @input="answerChanged"
              />
            </template>
            
            <!-- 简答题 -->
            <template v-else>
              <el-input 
                v-model="userAnswer" 
                type="textarea" 
                :rows="8"
                placeholder="请输入你的答案"
                @input="answerChanged"
              />
            </template>
          </div>
          
          <!-- 答案展示区 -->
          <div v-if="showAnswer" class="answer-display">
            <el-divider content-position="left">正确答案</el-divider>
            <div class="correct-answer">{{ displayCorrectAnswer }}</div>
            
            <el-divider content-position="left" v-if="currentQuestion?.analysis">题目解析</el-divider>
            <div v-if="currentQuestion?.analysis" class="analysis">{{ currentQuestion.analysis }}</div>
          </div>
          
          <!-- 操作按钮区 -->
          <div class="action-buttons">
            <el-button @click="prevQuestion" :disabled="currentIndex === 0" :icon="ArrowLeft">上一题</el-button>
            <el-button type="warning" @click="skipQuestion" :icon="Right">跳过</el-button>
            <el-button type="info" @click="toggleShowAnswer" :icon="View">
              {{ showAnswer ? '关闭答案' : '显示答案' }}
            </el-button>
            <el-button type="primary" @click="submitAnswer" :icon="CircleCheck">提交答案</el-button>
            <el-button @click="nextQuestion" :disabled="currentIndex === questions.length - 1" :icon="ArrowRight">下一题</el-button>
          </div>
        </div>
      </div>
      
      <!-- 右侧侧边栏 -->
      <div class="right-sidebar">
        <!-- 答题统计卡片 -->
        <div class="sidebar-card">
          <h3><el-icon><DataAnalysis /></el-icon> 答题统计</h3>
          <div class="stats-item">
            <span>已做题目</span>
            <span class="stat-number">{{ answeredCount }}</span>
          </div>
          <div class="stats-item">
            <span>正确率</span>
            <span class="stat-number">{{ accuracy }}%</span>
          </div>
          <div class="stats-item">
            <span>本次用时</span>
            <span class="stat-number">{{ formatTime(elapsedTime) }}</span>
          </div>
        </div>
        
        <!-- 题目索引 -->
        <div class="sidebar-card">
          <h3><el-icon><List /></el-icon> 题目索引</h3>
          <div class="question-index">
            <div 
              v-for="(q, idx) in questions" 
              :key="idx"
              class="index-item"
              :class="{
                'current': idx === currentIndex,
                'answered': userAnswers[idx],
                'wrong': wrongSet.has(idx),
                'skipped': skippedSet.has(idx)
              }"
              @click="goToQuestion(idx)"
            >
              {{ idx + 1 }}
            </div>
          </div>
        </div>
        
        <!-- 错题本 -->
        <div class="sidebar-card">
          <h3><el-icon><DocumentCopy /></el-icon> 错题本</h3>
          <div class="short-entry" @click="goToWrongBook">
            查看所有错题 <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        
        <!-- 笔记功能 -->
        <div class="sidebar-card">
          <h3><el-icon><EditPen /></el-icon> 笔记</h3>
          <el-input 
            v-model="questionNotes[currentQuestionId]" 
            type="textarea" 
            :rows="6"
            placeholder="给这道题写点笔记吧..."
            @blur="saveNotes"
          />
        </div>
        
        <!-- 评论区 -->
        <div class="sidebar-card">
          <h3><el-icon><ChatDotRound /></el-icon> 讨论区</h3>
          <div class="comment-list">
            <div class="comment-item" v-for="(com, idx) in questionComments" :key="idx">
              <div class="comment-user">{{ com.userName }}</div>
              <div class="comment-content">{{ com.content }}</div>
            </div>
          </div>
          <el-input 
            v-model="newComment" 
            placeholder="分享你的解题思路..."
            style="margin-top: 10px"
          >
            <template #append>
              <el-button @click="postComment">发送</el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 字体设置 -->
        <div class="sidebar-card">
          <h3><el-icon><Setting /></el-icon> 字体设置</h3>
          <div class="setting-item">
            <span>字体大小</span>
            <el-slider v-model="fontSize" :min="14" :max="24" style="width: 120px" />
          </div>
          <div class="setting-item">
            <span>护眼模式</span>
            <el-switch v-model="eyeCareMode" />
          </div>
        </div>
      </div>
    </div>
    
    <!-- 提交确认弹窗 -->
    <el-dialog
      v-model="submitConfirmVisible"
      title="确认提交"
      width="400px"
    >
      <p>确定要提交这道题的答案吗？</p>
      <template #footer>
        <el-button @click="submitConfirmVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

// 状态变量
const isDarkMode = ref(false)
const isFullscreen = ref(false)
const isTimerMode = ref(false)
const showAnswer = ref(false)
const submitConfirmVisible = ref(false)
const eyeCareMode = ref(false)
const fontSize = ref(16)

const categories = ref([])
const selectedCategory = ref(null)
const questions = ref([])
const currentIndex = ref(0)

const userAnswers = ref({})
const userAnswer = ref('')
const userMultiAnswer = ref([])
const wrongSet = ref(new Set())
const skippedSet = ref(new Set())
const isFavorite = ref(false)
const isWrongAnswer = ref(false)

const questionNotes = ref({})
const questionComments = ref([
  { userName: '学习达人', content: '这题我觉得用递归比较简单~' },
  { userName: '算法小白', content: '好难啊，看了解析才明白' }
])
const newComment = ref('')

const timerSeconds = ref(0)
const elapsedTime = ref(0)
let timerInterval = null
let startTime = null

// 计算属性
const currentQuestionId = computed(() => questions.value[currentIndex.value]?.id)
const currentQuestion = computed(() => questions.value[currentIndex.value])
const currentChoices = computed(() => currentQuestion.value?.choices || [])

const progressPercent = computed(() => {
  return Math.round(((currentIndex.value + 1) / questions.value.length) * 100)
})

const answeredCount = computed(() => Object.keys(userAnswers.value).length)

const accuracy = computed(() => {
  if (answeredCount.value === 0) return 0
  const correctCount = Object.keys(userAnswers.value).filter(idx => {
    const idxNum = parseInt(idx)
    return !wrongSet.value.has(idxNum) && !skippedSet.value.has(idxNum)
  }).length
  return Math.round((correctCount / answeredCount.value) * 100)
})

const displayCorrectAnswer = computed(() => {
  if (!currentQuestion.value) return ''
  const q = currentQuestion.value
  if (q.type === 'CHOICE') {
    const correctIndices = currentChoices.value
      .map((opt, idx) => opt.isCorrect ? idx : -1)
      .filter(idx => idx !== -1)
    return correctIndices.map(idx => String.fromCharCode(65 + idx)).join('、')
  } else {
    return q.answer?.answer || ''
  }
})

// 方法
const loadCategories = async () => {
  try {
    const res = await request.get('/api/categories')
    categories.value = res.data || []
    if (categories.value.length > 0) {
      selectedCategory.value = categories.value[0].id
      loadQuestions()
    }
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const loadQuestions = async () => {
  if (!selectedCategory.value) return
  try {
    const params = { categoryId: selectedCategory.value, page: 1, size: 50 }
    const res = await request.get('/api/questions/list', { params })
    questions.value = res.data.records || []
    currentIndex.value = 0
    userAnswers.value = {}
    wrongSet.value = new Set()
    skippedSet.value = new Set()
    userAnswer.value = ''
    userMultiAnswer.value = []
    showAnswer.value = false
    
    if (questions.value.length === 0) {
      ElMessage.warning('该分类下暂无题目')
    }
  } catch (e) {
    ElMessage.error('加载题目失败')
  }
}

const getDifficultyType = (diff) => {
  return diff === 'EASY' ? 'success' : diff === 'MEDIUM' ? 'warning' : 'danger'
}

const getDifficultyText = (diff) => {
  const map = { 'EASY': '简单', 'MEDIUM': '中等', 'HARD': '困难' }
  return map[diff] || diff
}

const selectChoice = (idx) => {
  userAnswer.value = idx
  answerChanged()
}

const toggleMultiChoice = (idx) => {
  const idxStr = idx
  const i = userMultiAnswer.value.indexOf(idxStr)
  if (i > -1) {
    userMultiAnswer.value.splice(i, 1)
  } else {
    userMultiAnswer.value.push(idxStr)
  }
  answerChanged()
}

const answerChanged = () => {
  // 保存当前题目的答案
  if (currentQuestion.value.type === 'CHOICE' && currentQuestion.value.multi) {
    userAnswers.value[currentIndex.value] = [...userMultiAnswer.value]
  } else {
    userAnswers.value[currentIndex.value] = userAnswer.value
  }
}

const toggleFavorite = () => {
  isFavorite.value = !isFavorite.value
  ElMessage.success(isFavorite.value ? '已收藏' : '已取消收藏')
  saveToLocalStorage()
}

const toggleWrongMark = () => {
  isWrongAnswer.value = !isWrongAnswer.value
  if (isWrongAnswer.value) {
    wrongSet.value.add(currentIndex.value)
  } else {
    wrongSet.value.delete(currentIndex.value)
  }
  ElMessage.success(isWrongAnswer.value ? '已标记为错题' : '已取消标记')
  saveToLocalStorage()
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    loadQuestionState()
  }
}

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    loadQuestionState()
  }
}

const skipQuestion = () => {
  skippedSet.value.add(currentIndex.value)
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    loadQuestionState()
  }
  saveToLocalStorage()
}

const goToQuestion = (idx) => {
  currentIndex.value = idx
  loadQuestionState()
}

const loadQuestionState = () => {
  const savedAnswer = userAnswers.value[currentIndex.value]
  if (currentQuestion.value.type === 'CHOICE' && currentQuestion.value.multi) {
    userMultiAnswer.value = savedAnswer ? [...savedAnswer] : []
  } else {
    userAnswer.value = savedAnswer || ''
  }
  showAnswer.value = false
  isWrongAnswer.value = wrongSet.value.has(currentIndex.value)
  // 从localStorage恢复笔记
  questionNotes.value[currentQuestionId.value] = 
    questionNotes.value[currentQuestionId.value] || ''
}

const toggleShowAnswer = () => {
  showAnswer.value = !showAnswer.value
}

const submitAnswer = () => {
  if (!userAnswer.value && 
      (currentQuestion.value.type !== 'CHOICE' || !currentQuestion.value.multi)) {
    ElMessage.warning('请先作答')
    return
  }
  if (currentQuestion.value.type === 'CHOICE' && 
      currentQuestion.value.multi && 
      userMultiAnswer.value.length === 0) {
    ElMessage.warning('请选择答案')
    return
  }
  submitConfirmVisible.value = true
}

const confirmSubmit = () => {
  submitConfirmVisible.value = false
  showAnswer.value = true
  
  // 简单的自动判分（只适用于客观题）
  if (currentQuestion.value.type !== 'TEXT') {
    let isCorrect = checkAnswer()
    if (!isCorrect) {
      wrongSet.value.add(currentIndex.value)
    }
    ElMessage.success(isCorrect ? '回答正确！' : '回答错误，查看解析学习吧')
  } else {
    ElMessage.success('答案已保存，简答题暂不支持自动判分')
  }
  
  saveToLocalStorage()
}

const checkAnswer = () => {
  if (!currentQuestion.value) return false
  const q = currentQuestion.value
  
  if (q.type === 'CHOICE' && q.multi) {
    const correctIndices = currentChoices.value
      .map((opt, idx) => opt.isCorrect ? idx : -1)
      .filter(idx => idx !== -1)
      .sort()
    const userIndices = userMultiAnswer.value.map(x => parseInt(x)).sort()
    return JSON.stringify(correctIndices) === JSON.stringify(userIndices)
  } else if (q.type === 'CHOICE') {
    const correctIdx = currentChoices.value.findIndex(opt => opt.isCorrect)
    return parseInt(userAnswer.value) === correctIdx
  } else if (q.type === 'JUDGE') {
    return userAnswer.value === q.answer?.answer
  } else if (q.type === 'FILL') {
    return userAnswer.value.trim() === q.answer?.answer?.trim()
  }
  return false
}

const saveNotes = () => {
  if (currentQuestionId.value) {
    saveToLocalStorage()
    ElMessage.success('笔记已保存')
  }
}

const postComment = () => {
  if (!newComment.value.trim()) return
  questionComments.value.unshift({
    userName: '我',
    content: newComment.value
  })
  newComment.value = ''
  ElMessage.success('评论发送成功')
}

const goToWrongBook = () => {
  ElMessage.info('跳转错题本功能开发中...')
}

const goBack = () => {
  router.push('/home')
}

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 本地存储相关
const loadFromLocalStorage = () => {
  const data = localStorage.getItem('questionStudyData')
  if (data) {
    const parsed = JSON.parse(data)
    // 根据当前分类加载对应数据
  }
}

const saveToLocalStorage = () => {
  const data = {
    userAnswers: userAnswers.value,
    wrongSet: Array.from(wrongSet.value),
    skippedSet: Array.from(skippedSet.value),
    questionNotes: questionNotes.value,
    lastCategory: selectedCategory.value
  }
  localStorage.setItem('questionStudyData', JSON.stringify(data))
}

// 计时器
const startTimer = () => {
  startTime = Date.now()
  timerInterval = setInterval(() => {
    elapsedTime.value = Math.floor((Date.now() - startTime) / 1000)
  }, 1000)
}

// 生命周期
onMounted(() => {
  loadCategories()
  loadFromLocalStorage()
  startTimer()
  
  // 监听全屏变化
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })
})

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
  }
  saveToLocalStorage()
})
</script>

<style scoped>
.question-study-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  transition: background 0.3s;
}

.dark-mode {
  background: #1a1a1a;
  color: #e5e5e5;
}

.dark-mode .top-nav-bar {
  background: #2d2d2d;
}

.dark-mode .question-display {
  background: #2d2d2d;
}

.dark-mode .sidebar-card {
  background: #2d2d2d;
}

/* 顶部导航栏 */
.top-nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.question-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-weight: 500;
}

.difficulty-tag {
  font-size: 14px;
}

.nav-right {
  display: flex;
  gap: 8px;
}

/* 主内容区域 */
.main-content {
  display: flex;
  padding: 24px;
  gap: 24px;
  flex: 1;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.left-section {
  flex: 1;
  min-width: 0;
}

.right-sidebar {
  width: 320px;
  flex-shrink: 0;
}

/* 计时器 */
.timer-bar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
}

.timer-text {
  font-family: monospace;
  font-size: 20px;
}

/* 题目展示 */
.question-display {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.question-title {
  font-size: 18px;
  line-height: 1.8;
  margin-bottom: 24px;
  color: #303133;
}

.dark-mode .question-title {
  color: #e5e5e5;
}

.code-block {
  background: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
}

.code-block pre {
  margin: 0;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.6;
}

.question-image {
  margin-bottom: 24px;
  text-align: center;
}

.question-image img {
  max-width: 100%;
  border-radius: 8px;
}

/* 答题区域 */
.answer-area {
  margin-bottom: 24px;
}

.choice-option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin: 8px 0;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.choice-option:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.choice-option.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.choice-option.correct {
  border-color: #67c23a;
  background: #f0f9eb;
}

.choice-option.wrong {
  border-color: #f56c6c;
  background: #fef0f0;
}

.choice-label {
  font-weight: 700;
  color: #606266;
  min-width: 24px;
}

.judge-options {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 答案展示 */
.answer-display {
  background: #f9fafc;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.correct-answer {
  font-weight: 600;
  color: #67c23a;
  font-size: 16px;
}

.analysis {
  color: #606266;
  line-height: 1.8;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

/* 侧边栏卡片 */
.sidebar-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.sidebar-card h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dark-mode .sidebar-card h3 {
  color: #e5e5e5;
}

.stats-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-number {
  font-weight: 700;
  color: #409eff;
  font-size: 18px;
}

.question-index {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.index-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.index-item:hover {
  border-color: #409eff;
}

.index-item.current {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.index-item.answered {
  border-color: #67c23a;
  background: #f0f9eb;
  color: #67c23a;
}

.index-item.wrong {
  border-color: #f56c6c;
  background: #fef0f0;
  color: #f56c6c;
}

.index-item.skipped {
  border-color: #e6a23c;
  background: #fdf6ec;
  color: #e6a23c;
}

.short-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.short-entry:hover {
  background: #ecf5ff;
  color: #409eff;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0;
}

.comment-list {
  max-height: 150px;
  overflow-y: auto;
}

.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-user {
  font-weight: 600;
  font-size: 13px;
  color: #409eff;
  margin-bottom: 4px;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

/* 护眼模式 */
.eye-care-mode .question-display {
  background: #f5f1e3 !important;
}

.eye-care-mode .question-title {
  color: #3c3c3c;
}

/* 全屏样式 */
.fullscreen .top-nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
}

.fullscreen .main-content {
  padding-top: 100px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .right-sidebar {
    display: none;
  }
}
</style>
