<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listNotifications, markAllRead, markRead } from '../../api/notification'
import { useNotificationStore } from '../../stores/notification'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const notificationStore = useNotificationStore()
const loading = ref(false)
const list = ref([])
const pager = reactive({ page: 1, size: 20, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const pageData = await listNotifications({ page: pager.page, size: pager.size })
    list.value = pageData.records || []
    pager.total = pageData.total || 0
    await notificationStore.refreshUnreadCount()
  } finally {
    loading.value = false
  }
}

const goTarget = async (row) => {
  if (!row.isRead) {
    await markRead(row.id)
  }
  if (row.refType === 'POST' && row.refId) {
    await router.push(`/posts/${row.refId}`)
  }
  if (row.refType === 'THREAD' && row.refId) {
    await router.push(`/threads/${row.refId}`)
  }
  await fetchData()
}

const handleMarkAll = async () => {
  await markAllRead()
  ElMessage.success('已全部标记已读')
  await fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h2 class="page-title" style="margin-bottom: 0">通知中心</h2>
        <el-button @click="handleMarkAll">全部已读</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.type === 'MATCH' ? 'warning' : row.type === 'MESSAGE' ? 'success' : 'info'"
              size="small"
            >
              {{ { MATCH: '匹配通知', MESSAGE: '消息通知', SYSTEM: '系统通知' }[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="200" />
        <el-table-column label="内容" prop="content" min-width="260" />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'info' : 'danger'">{{ row.isRead ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="120">
          <template #default="{ row }">
            <el-button type="primary" text @click="goTarget(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无通知" style="padding: 32px 0" />
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
