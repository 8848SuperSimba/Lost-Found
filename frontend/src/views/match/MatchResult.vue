<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createThread } from '../../api/thread'
import { getMatches, rematch } from '../../api/match'
import { getPostDetail } from '../../api/post'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rematching = ref(false)
const matches = ref([])
const sourcePost = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    sourcePost.value = await getPostDetail(route.params.id)
    matches.value = await getMatches(route.params.id)
  } finally {
    loading.value = false
  }
}

const handleRematch = async () => {
  rematching.value = true
  try {
    await rematch(route.params.id)
    ElMessage.success('重跑匹配完成')
    await loadData()
  } finally {
    rematching.value = false
  }
}

const handleCreateThread = async (targetPostId) => {
  if (!sourcePost.value) return
  try {
    const payload =
      sourcePost.value.postType === 'LOST'
        ? { lostPostId: sourcePost.value.id, foundPostId: targetPostId }
        : { lostPostId: targetPostId, foundPostId: sourcePost.value.id }
    const thread = await createThread(payload)
    ElMessage.success('会话建立成功')
    await router.push(`/threads/${thread.id}`)
  } catch {
    // 拦截器处理错误
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h2 class="page-title" style="margin-bottom: 0">匹配结果</h2>
        <el-button type="primary" :loading="rematching" @click="handleRematch">重新匹配</el-button>
      </div>
      <div v-if="sourcePost" style="color: #606266; font-size: 14px; margin: 8px 0 12px">
        帖子：{{ sourcePost.title }}
        <el-tag size="small" style="margin-left: 8px">
          {{ sourcePost.postType === 'LOST' ? '失物' : '寻物' }}
        </el-tag>
      </div>
      <el-table :data="matches" v-loading="loading">
        <el-table-column label="相似度" width="120">
          <template #default="{ row }">{{ row.scorePercent }}%</template>
        </el-table-column>
        <el-table-column label="匹配帖子" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/posts/${row.post.id}`)">{{ row.post.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="匹配说明" min-width="300">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag v-for="reason in row.matchReasons || []" :key="reason" type="success">{{ reason }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button type="primary" text @click="handleCreateThread(row.post.id)">发起联系</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!loading && matches.length === 0"
        description="暂时没有找到匹配结果，可尝试重新匹配"
        style="padding: 40px 0"
      />
    </div>
  </div>
</template>
