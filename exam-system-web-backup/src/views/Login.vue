<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h2>{{ isRegister ? '注册账号' : '登录账号' }}</h2>
        <p class="subtitle">{{ isRegister ? '创建新账号，开始学习之旅' : '欢迎回来，继续学习' }}</p>
      </div>

      <!-- 登录方式切换 -->
      <div class="login-tabs">
        <el-radio-group v-model="loginType" @change="handleLoginTypeChange">
          <el-radio-button label="email">邮箱验证码登录</el-radio-button>
          <el-radio-button label="account">账号密码登录</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 邮箱登录表单 -->
      <el-form v-if="loginType === 'email'" :model="emailForm" :rules="emailRules" ref="emailFormRef" class="login-form">
        <el-form-item prop="email">
          <el-input
            v-model="emailForm.email"
            placeholder="请输入邮箱地址"
            size="large"
            :prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item prop="code" v-if="!isRegister">
          <el-input
            v-model="emailForm.code"
            placeholder="请输入验证码"
            size="large"
            :prefix-icon="CircleCheck"
            style="width: 60%"
          />
          <el-button
            size="large"
            :disabled="countdown > 0"
            @click="sendCode"
            style="width: 38%; margin-left: 2%"
          >
            {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
          </el-button>
        </el-form-item>

        <el-form-item prop="password" v-if="isRegister">
          <el-input
            v-model="emailForm.password"
            type="password"
            placeholder="设置密码（6位以上）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword" v-if="isRegister">
          <el-input
            v-model="emailForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleEmailLogin"
            class="login-btn"
          >
            {{ isRegister ? '立即注册' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 账号登录表单 -->
      <el-form v-else :model="accountForm" :rules="accountRules" ref="accountFormRef" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="accountForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="accountForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleAccountLogin"
            class="login-btn"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 切换登录/注册 -->
      <div class="toggle-mode">
        <span v-if="!isRegister">还没有账号？</span>
        <span v-else>已有账号？</span>
        <el-link type="primary" @click="isRegister = !isRegister">
          {{ isRegister ? '立即登录' : '立即注册' }}
        </el-link>
      </div>

      <!-- 返回首页 -->
      <div class="back-home">
        <el-link type="info" @click="goHome" :icon="HomeFilled">返回首页</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const loginType = ref('email')
const isRegister = ref(false)
const loading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const emailFormRef = ref()
const accountFormRef = ref()

const emailForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const accountForm = reactive({
  username: '',
  password: ''
})

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== emailForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const accountRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLoginTypeChange = () => {
  isRegister.value = false
  emailFormRef.value?.resetFields()
  accountFormRef.value?.resetFields()
}

const sendCode = async () => {
  if (!emailForm.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  if (!/^[\w.-]+@[\w.-]+\.\w+$/.test(emailForm.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  try {
    await request.post('/api/auth/send-code', null, {
      params: { email: emailForm.email, type: 'LOGIN' }
    })
    ElMessage.success('验证码已发送，请查收')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } catch (e) {
    ElMessage.error('验证码发送失败')
  }
}

const handleEmailLogin = async () => {
  if (isRegister.value) {
    // 注册流程：先发送验证码
    if (!emailForm.password || emailForm.password.length < 6) {
      ElMessage.warning('请设置6位以上的密码')
      return
    }
    if (!emailForm.confirmPassword) {
      ElMessage.warning('请确认密码')
      return
    }
    // 注册时也需要验证码，先让用户获取验证码
    ElMessage.info('请先获取验证码完成注册')
    return
  }

  // 登录流程
  if (!emailForm.email || !emailForm.code) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/api/auth/login-email', {
      loginType: 'EMAIL',
      email: emailForm.email,
      code: emailForm.code
    })
    
    // 保存用户信息
    localStorage.setItem('dengluUser', JSON.stringify(res.data))
    localStorage.setItem('dengluUserId', res.data.id)
    
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    // 错误由拦截器处理
  } finally {
    loading.value = false
  }
}

const handleAccountLogin = async () => {
  if (!accountForm.username || !accountForm.password) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/api/auth/login-account', {
      loginType: 'ACCOUNT',
      username: accountForm.username,
      password: accountForm.password
    })
    
    localStorage.setItem('dengluUser', JSON.stringify(res.data))
    localStorage.setItem('dengluUserId', res.data.id)
    
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    // 错误由拦截器处理
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push('/home')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  background: white;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 450px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 28px;
}

.subtitle {
  color: #999;
  margin: 0;
  font-size: 14px;
}

.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.login-form {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
}

.toggle-mode {
  text-align: center;
  color: #666;
  margin-top: 20px;
}

.back-home {
  text-align: center;
  margin-top: 20px;
}
</style>
