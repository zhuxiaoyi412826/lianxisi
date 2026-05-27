<template>
  <div class="profile-page">
    <!-- 顶部导航 -->
    <div class="profile-nav">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h2>个人中心</h2>
      <el-button type="danger" @click="handleLogout" plain>退出登录</el-button>
    </div>

    <div class="profile-container">
      <!-- 左侧个人信息卡片 -->
      <div class="profile-card">
        <div class="avatar-section">
          <el-avatar :size="100" :src="userForm.avatar || defaultAvatar">
            {{ userForm.realName?.charAt(0) || userForm.username?.charAt(0) || '用户' }}
          </el-avatar>
          <el-upload
            class="avatar-upload"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="uploadAvatar"
          >
            <el-button size="small" type="primary">更换头像</el-button>
          </el-upload>
        </div>

        <div class="user-info-basic">
          <h3>{{ userForm.realName || userForm.username }}</h3>
          <p class="student-no">学号：{{ userForm.studentNo }}</p>
          <el-tag :type="getStatusType(userForm.status)">
            {{ getStatusText(userForm.status) }}
          </el-tag>
        </div>

        <el-divider />

        <div class="user-stats">
          <div class="stat-item">
            <span class="stat-value">{{ userForm.loginType === 'EMAIL' ? '邮箱' : '账号' }}</span>
            <span class="stat-label">登录方式</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ formatDate(userForm.lastLoginTime) }}</span>
            <span class="stat-label">最后登录</span>
          </div>
        </div>
      </div>

      <!-- 右侧编辑表单 -->
      <div class="profile-form-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form :model="userForm" label-width="100px" class="info-form">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="用户名">
                    <el-input v-model="userForm.username" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="学号">
                    <el-input v-model="userForm.studentNo" disabled />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="真实姓名">
                    <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="性别">
                    <el-radio-group v-model="userForm.gender">
                      <el-radio label="male">男</el-radio>
                      <el-radio label="female">女</el-radio>
                      <el-radio label="other">其他</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="出生日期">
                    <el-date-picker
                      v-model="userForm.birthday"
                      type="date"
                      placeholder="选择日期"
                      style="width: 100%"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="手机号码">
                    <el-input v-model="userForm.phone" placeholder="请输入手机号码" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="邮箱">
                <el-input v-model="userForm.email" disabled />
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="专业">
                    <el-input v-model="userForm.major" placeholder="请输入专业" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="年级">
                    <el-input v-model="userForm.grade" placeholder="如：2024级" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="班级">
                <el-input v-model="userForm.className" placeholder="请输入班级" />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="saveProfile" :loading="saving">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px" class="password-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（6位以上）"
                  show-password
                />
              </el-form-item>

              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="changePassword" :loading="changingPwd">
                  修改密码
                </el-button>
                <el-button @click="passwordFormRef?.resetFields()">重置</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const activeTab = ref('basic')
const saving = ref(false)
const changingPwd = ref(false)
const defaultAvatar = ref('')

const userForm = reactive({
  id: null,
  username: '',
  studentNo: '',
  email: '',
  realName: '',
  phone: '',
  avatar: '',
  gender: '',
  birthday: '',
  major: '',
  grade: '',
  className: '',
  status: '',
  loginType: '',
  lastLoginTime: ''
})

const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadUserInfo = () => {
  const userId = localStorage.getItem('dengluUserId')
  if (!userId) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }

  request.get(`/api/auth/current-user`, {
    headers: { 'X-User-Id': userId }
  }).then(res => {
    Object.assign(userForm, res.data)
  }).catch(() => {
    ElMessage.error('获取用户信息失败')
  })
}

const saveProfile = async () => {
  saving.value = true
  try {
    const userId = localStorage.getItem('dengluUserId')
    await request.put('/api/auth/update-profile', userForm, {
      headers: { 'X-User-Id': userId }
    })
    ElMessage.success('保存成功')
    // 更新本地存储
    localStorage.setItem('dengluUser', JSON.stringify(userForm))
  } catch (e) {
    // 错误由拦截器处理
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    changingPwd.value = true
    try {
      const userId = localStorage.getItem('dengluUserId')
      await request.put('/api/auth/change-password', null, {
        params: {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        },
        headers: { 'X-User-Id': userId }
      })
      ElMessage.success('密码修改成功')
      passwordFormRef.value.resetFields()
    } catch (e) {
      // 错误由拦截器处理
    } finally {
      changingPwd.value = false
    }
  })
}

const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
    return false
  }
  return true
}

const uploadAvatar = (options) => {
  const formData = new FormData()
  formData.append('file', options.file)

  request.post('/api/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => {
    userForm.avatar = res.data
    ElMessage.success('头像上传成功')
  }).catch(() => {
    ElMessage.error('头像上传失败')
  })
}

const getStatusType = (status) => {
  const map = { 'ACTIVE': 'success', 'INACTIVE': 'warning', 'BANNED': 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 'ACTIVE': '正常', 'INACTIVE': '未激活', 'BANNED': '已封禁' }
  return map[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return '未知'
  return dateStr.substring(0, 16).replace('T', ' ')
}

const handleLogout = () => {
  localStorage.removeItem('dengluUser')
  localStorage.removeItem('dengluUserId')
  ElMessage.success('已退出登录')
  router.push('/home')
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.profile-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.profile-nav h2 {
  margin: 0;
  color: #333;
}

.profile-container {
  display: flex;
  gap: 24px;
  padding: 24px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card {
  width: 300px;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  flex-shrink: 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.user-info-basic {
  text-align: center;
  margin-top: 20px;
}

.user-info-basic h3 {
  margin: 0 0 8px 0;
  color: #333;
}

.student-no {
  color: #999;
  font-size: 14px;
  margin: 8px 0;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.profile-form-card {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.info-form,
.password-form {
  max-width: 600px;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .profile-container {
    flex-direction: column;
    padding: 16px;
  }

  .profile-card {
    width: 100%;
  }
}
</style>
