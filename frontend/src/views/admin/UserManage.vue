<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsers, updateUserRole, updateUserStatus } from '../../api/user'
import { formatDateTime } from '../../utils/format'
import { useAuthStore } from '../../stores/auth'

const loading = ref(false)
const users = ref([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ keyword: '', status: '' })
const authStore = useAuthStore()

const fetchData = async () => {
  loading.value = true
  try {
    const pageData = await getAdminUsers({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      page: pager.page,
      size: pager.size,
    })
    users.value = pageData.records || []
    pager.total = pageData.total || 0
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row) => {
  const status = row.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE'
  await updateUserStatus(row.id, status)
  ElMessage.success(status === 'BANNED' ? '已封禁' : '已解封')
  await fetchData()
}

const toggleRole = async (row) => {
  const role = row.role === 'ADMIN' ? 'USER' : 'ADMIN'
  await updateUserRole(row.id, role)
  ElMessage.success(role === 'ADMIN' ? '已设为普通管理员' : '已降为普通用户')
  await fetchData()
}

const canToggleRole = (row) => {
  if (!authStore.isSuperAdmin) return false
  if (!row || row.role === 'SUPER_ADMIN') return false
  return row.id !== authStore.user?.id
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">管理员 - 用户管理</h2>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="用户名/手机号" style="width: 220px" clearable />
        <el-select v-model="query.status" placeholder="状态" style="width: 140px" clearable>
          <el-option label="ACTIVE" value="ACTIVE" />
          <el-option label="BANNED" value="BANNED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <el-table :data="users" v-loading="loading">
        <el-table-column label="ID" prop="id" width="90" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.avatarUrl">{{ row.nickname?.slice(0, 1) || row.username?.slice(0, 1) || 'U' }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="用户名" prop="username" width="140" />
        <el-table-column label="昵称" prop="nickname" width="140" />
        <el-table-column label="手机号" prop="phone" width="140" />
        <el-table-column label="角色" prop="role" width="130" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button type="primary" text @click="toggleStatus(row)">{{ row.status === 'ACTIVE' ? '封禁' : '解封' }}</el-button>
            <el-button v-if="canToggleRole(row)" type="warning" text @click="toggleRole(row)">
              {{ row.role === 'ADMIN' ? '降为用户' : '设为管理员' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>
