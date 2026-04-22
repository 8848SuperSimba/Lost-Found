<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { closePost, listPosts, resolvePost } from '../../api/post'
import { useAuthStore } from '../../stores/auth'
import { STATUS_LABEL } from '../../utils/dict'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const authStore = useAuthStore()
const posts = ref([])
const loading = ref(false)

const loadMyPosts = async () => {
  loading.value = true
  try {
    const page = await listPosts({ page: 1, size: 100 })
    const records = page.records || []
    posts.value = records.filter((item) => item.publisherNickname === authStore.user?.nickname)
  } finally {
    loading.value = false
  }
}

const closeCurrentPost = async (row) => {
  await ElMessageBox.confirm(`确认关闭「${row.title}」吗？`, '提示', { type: 'warning' })
  await closePost(row.id)
  ElMessage.success('已关闭')
  await loadMyPosts()
}

const markResolved = async (row) => {
  await resolvePost(row.id)
  ElMessage.success('已标记找回')
  await loadMyPosts()
}

onMounted(loadMyPosts)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">我的帖子</h2>
      <el-table :data="posts" v-loading="loading">
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/posts/${row.id}`)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">{{ STATUS_LABEL[row.status] || row.status }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-space>
              <el-button text type="primary" @click="router.push(`/posts/${row.id}/edit`)">编辑</el-button>
              <el-button text @click="router.push(`/my/posts/${row.id}/matches`)">匹配</el-button>
              <el-button text type="success" @click="markResolved(row)">标记找回</el-button>
              <el-button text type="danger" @click="closeCurrentPost(row)">关闭</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
