<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listThreads } from '../../api/thread'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const threads = ref([])
const loading = ref(false)
let timer = null

const fetchThreads = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    threads.value = await listThreads()
  } finally {
    if (showLoading) loading.value = false
  }
}

onMounted(async () => {
  await fetchThreads(true)
  timer = setInterval(() => fetchThreads(false), 5000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">我的会话</h2>
      <el-empty v-if="threads.length === 0 && !loading" description="暂无会话" />
      <el-table v-else :data="threads" v-loading="loading">
        <el-table-column label="对方" width="160">
          <template #default="{ row }">
            <el-space>
              <el-avatar :src="row.otherAvatar" />
              <span>{{ row.otherNickname || `用户${row.otherUserId}` }}</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="关联帖子" prop="relatedPostTitle" min-width="180" />
        <el-table-column label="最后消息" prop="lastMessageContent" min-width="240" />
        <el-table-column label="未读" width="90">
          <template #default="{ row }">
            <el-badge :value="row.unreadCount || 0" :max="99" :hidden="!row.unreadCount" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastMessageTime) }}</template>
        </el-table-column>
        <el-table-column width="120">
          <template #default="{ row }">
            <el-button type="primary" text @click="router.push(`/threads/${row.id}`)">进入会话</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
