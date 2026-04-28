<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getThreadDetail, getThreadMessages, sendMessage } from '../../api/thread'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const messages = ref([])
const input = ref('')
const listRef = ref()
let timer = null

const loadDetail = async () => {
  detail.value = await getThreadDetail(route.params.id)
}

const loadMessages = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const page = await getThreadMessages(route.params.id, { page: 1, size: 200 })
    messages.value = page.records || []
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  } finally {
    if (!silent) loading.value = false
  }
}

const handleSend = async () => {
  const content = input.value.trim()
  if (!content) return
  const msg = await sendMessage(route.params.id, { content })
  messages.value.push(msg)
  input.value = ''
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

onMounted(async () => {
  await loadDetail()
  await loadMessages(false)
  timer = setInterval(() => loadMessages(true), 5000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title" style="margin-bottom: 8px">{{ detail?.relatedPostTitle || '会话详情' }}</h2>
      <div style="margin: 0 0 12px; color: #606266; font-size: 14px">
        <span>对方联系方式：{{ detail?.otherContactInfo || '对方未填写联系方式' }}</span>
        <el-divider direction="vertical" />
        <span>我的联系方式：{{ detail?.myContactInfo || '我未填写联系方式（可在帖子中补充）' }}</span>
      </div>
      <div
        ref="listRef"
        v-loading="loading"
        style="height: 420px; overflow-y: auto; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px; background: #fafafa"
      >
        <div
          v-for="item in messages"
          :key="item.id"
          :style="{
            display: 'flex',
            justifyContent: item.isSelf ? 'flex-end' : 'flex-start',
            marginBottom: '8px',
          }"
        >
          <div
            :style="{
              maxWidth: '70%',
              background: item.isSelf ? '#409eff' : '#fff',
              color: item.isSelf ? '#fff' : '#303133',
              border: item.isSelf ? 'none' : '1px solid #ebeef5',
              borderRadius: '8px',
              padding: '8px 10px',
            }"
          >
            <div style="font-size: 12px; opacity: 0.8">{{ item.senderNickname || `用户${item.senderUserId}` }}</div>
            <div style="white-space: pre-wrap">{{ item.content }}</div>
            <div style="font-size: 12px; opacity: 0.75; margin-top: 2px">{{ formatDateTime(item.createdAt) }}</div>
          </div>
        </div>
      </div>

      <el-alert v-if="detail?.status === 'CLOSED'" title="会话已关闭，无法发送消息" type="warning" style="margin-top: 12px" />
      <div style="margin-top: 8px" v-if="detail?.status === 'CLOSED'">
        <el-button @click="router.push('/threads')">返回会话列表</el-button>
      </div>
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        :disabled="detail?.status === 'CLOSED'"
        placeholder="输入消息，Enter发送，Shift+Enter换行"
        style="margin-top: 12px"
        @keydown.enter.exact.prevent="handleSend"
      />
      <div style="margin-top: 10px; text-align: right">
        <el-button type="primary" :disabled="detail?.status === 'CLOSED'" @click="handleSend">发送消息</el-button>
      </div>
    </div>
  </div>
</template>
