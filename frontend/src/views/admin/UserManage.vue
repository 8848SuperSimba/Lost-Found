<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsers, updateUserStatus } from '../../api/user'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const users = ref([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ keyword: '', status: '' })

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
        <el-table-column label="用户名" prop="username" width="140" />
        <el-table-column label="昵称" prop="nickname" width="140" />
        <el-table-column label="手机号" prop="phone" width="140" />
        <el-table-column label="角色" prop="role" width="110" />
        <el-table-column label="状态" prop="status" width="110" />
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" text @click="toggleStatus(row)">{{ row.status === 'ACTIVE' ? '封禁' : '解封' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
