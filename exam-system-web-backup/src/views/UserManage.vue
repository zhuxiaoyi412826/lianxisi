<template>
  <div class="user-manage-page">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名/邮箱/姓名/学号"
        style="width: 300px"
        clearable
        @clear="loadUsers"
        @keyup.enter="loadUsers"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="statusFilter" placeholder="状态筛选" style="width: 150px" clearable>
        <el-option label="正常" value="ACTIVE" />
        <el-option label="未激活" value="INACTIVE" />
        <el-option label="已封禁" value="BANNED" />
      </el-select>

      <el-button type="primary" @click="loadUsers" :icon="Search">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- 用户列表 -->
    <div class="user-table">
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="100" />
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="major" label="专业" width="120" />
        <el-table-column label="性别" width="60">
          <template #default="{ row }">
            {{ row.gender === 'male' ? '男' : row.gender === 'female' ? '女' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="登录方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.loginType === 'EMAIL' ? 'success' : 'primary'" size="small">
              {{ row.loginType === 'EMAIL' ? '邮箱' : '账号' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">
            {{ row.lastLoginTime ? formatDate(row.lastLoginTime) : '从未登录' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
            <el-dropdown @command="handleCommand($event, row)">
              <el-button size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="enable" v-if="row.status !== 'ACTIVE'">
                    <el-icon><Check /></el-icon> 启用账号
                  </el-dropdown-item>
                  <el-dropdown-item command="disable" v-if="row.status === 'ACTIVE'">
                    <el-icon><Close /></el-icon> 禁用账号
                  </el-dropdown-item>
                  <el-dropdown-item command="ban" v-if="row.status !== 'BANNED'">
                    <el-icon><CircleClose /></el-icon> 封禁账号
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon> 删除用户
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </div>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <div class="user-detail" v-if="currentUser">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ currentUser.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ currentUser.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ currentUser.gender === 'male' ? '男' : currentUser.gender === 'female' ? '女' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱" :span="2">{{ currentUser.email }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ currentUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentUser.status)">
              {{ getStatusText(currentUser.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="专业">{{ currentUser.major || '-' }}</el-descriptions-item>
          <el-descriptions-item label="年级">{{ currentUser.grade || '-' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ currentUser.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录方式">
            {{ currentUser.loginType === 'EMAIL' ? '邮箱' : '账号' }}
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatDate(currentUser.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录IP">{{ currentUser.lastLoginIp || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const userList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const statusFilter = ref('')

const detailVisible = ref(false)
const currentUser = ref(null)

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      status: statusFilter.value
    }
    const res = await request.get('/api/admin/users/list', { params })
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  searchKeyword.value = ''
  statusFilter.value = ''
  loadUsers()
}

const viewDetail = async (row) => {
  try {
    const res = await request.get(`/api/admin/users/${row.id}`)
    currentUser.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取用户详情失败')
  }
}

const handleCommand = async (command, row) => {
  switch (command) {
    case 'enable':
      await updateStatus(row.id, 'ACTIVE', '启用')
      break
    case 'disable':
      await updateStatus(row.id, 'INACTIVE', '禁用')
      break
    case 'ban':
      await updateStatus(row.id, 'BANNED', '封禁')
      break
    case 'delete':
      await deleteUser(row.id)
      break
  }
}

const updateStatus = async (id, status, action) => {
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示')
    await request.put(`/api/admin/users/${id}/status`, null, {
      params: { status }
    })
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？此操作不可恢复！', '警告', {
      type: 'warning'
    })
    await request.delete(`/api/admin/users/${id}`)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
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
  if (!dateStr) return '-'
  return dateStr.substring(0, 19).replace('T', ' ')
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-manage-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.user-table {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.user-detail {
  padding: 10px 0;
}
</style>
