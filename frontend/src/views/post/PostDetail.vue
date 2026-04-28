<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostDetail } from '../../api/post'
import { getMatches } from '../../api/match'
import { createThread } from '../../api/thread'
import { useAuthStore } from '../../stores/auth'
import { CATEGORY_LABEL, POST_TYPE_LABEL, STATUS_LABEL } from '../../utils/dict'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const detail = ref(null)
const loading = ref(false)
const matches = ref([])

const isOwner = computed(
  () =>
    authStore.user?.id &&
    detail.value?.publisherUserId &&
    Number(authStore.user.id) === Number(detail.value.publisherUserId),
)

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await getPostDetail(route.params.id)
    if (authStore.token) {
      try {
        matches.value = await getMatches(route.params.id)
      } catch (error) {
        matches.value = []
      }
    }
  } finally {
    loading.value = false
  }
}

const openCreateThread = async (matchedPostId) => {
  if (!authStore.token) {
    ElMessage.warning('请先登录后再联系对方')
    return
  }
  if (!detail.value) return
  try {
    const payload =
      detail.value.postType === 'LOST'
        ? { lostPostId: detail.value.id, foundPostId: matchedPostId }
        : { lostPostId: matchedPostId, foundPostId: detail.value.id }
    const thread = await createThread(payload)
    ElMessage.success('会话建立成功，即将跳转到聊天页')
    await router.push(`/threads/${thread.id}`)
  } catch {
    // HTTP 拦截器已处理错误提示
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="card-block" v-if="detail">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h2 class="page-title" style="margin: 0">{{ detail.title }}</h2>
        <el-space>
          <el-tag>{{ POST_TYPE_LABEL[detail.postType] || detail.postType }}</el-tag>
          <el-tag type="warning">{{ STATUS_LABEL[detail.status] || detail.status }}</el-tag>
        </el-space>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="分类">{{ CATEGORY_LABEL[detail.category] || detail.category }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ detail.areaText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="遗失/拾获时间">{{ formatDateTime(detail.lostFoundTime) }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="发布者">{{ detail.publisherNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.contactInfo || '建立会话后可见' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <p style="white-space: pre-wrap">{{ detail.description || '-' }}</p>
      <div style="margin-top: 12px">
        <el-space wrap>
          <el-tag v-for="keyword in detail.keywords || []" :key="keyword">{{ keyword }}</el-tag>
        </el-space>
      </div>
      <div style="margin-top: 12px">
        <el-image
          v-for="(img, idx) in detail.imageUrls || []"
          :key="idx"
          :src="img"
          style="width: 120px; height: 120px; margin-right: 8px"
          fit="cover"
        />
      </div>
      <div style="margin-top: 16px" v-if="isOwner">
        <el-button type="primary" @click="router.push(`/posts/${detail.id}/edit`)">编辑帖子</el-button>
        <el-button @click="router.push(`/my/posts/${detail.id}/matches`)">查看匹配结果</el-button>
      </div>
    </div>

    <div class="card-block">
      <h3 style="margin-top: 0">智能匹配推荐</h3>
      <el-empty v-if="matches.length === 0" description="暂无匹配结果或无查看权限" />
      <el-table v-else :data="matches.slice(0, 5)">
        <el-table-column label="匹配分" width="120">
          <template #default="{ row }">{{ row.scorePercent }}%</template>
        </el-table-column>
        <el-table-column label="匹配帖子" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/posts/${row.post.id}`)">{{ row.post.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="原因" min-width="260">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag v-for="reason in row.matchReasons || []" :key="reason" type="success">{{ reason }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column width="140">
          <template #default="{ row }">
            <el-button type="primary" text @click="openCreateThread(row.post.id)">
              {{ detail?.postType === 'LOST' ? '我捡到了，联系失主' : '这是我丢的，联系拾主' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
