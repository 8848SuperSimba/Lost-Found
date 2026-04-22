<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getThreadDetail, getThreadMessages, sendMessage } from '../../api/thread'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const loading = ref(false)
const detail = ref(null)
const messages = ref([])
const input = ref('')
const listRef = ref()
let timer = null

const loadDetail = async () => {
  detail.value = await getThreadDetail(route.params.id)
}

const loadMessages = async () => {
  loading.value = true
  try {
    const page = await getThreadMessages(route.params.id, { page: 1, size: 200 })
    messages.value = page.records || []
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  } finally {
    loading.value = false
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
  await loadMessages()
  timer = setInterval(loadMessages, 5000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title" style="margin-bottom: 8px">{{ detail?.relatedPostTitle || '会话详情' }}</h2>
      <p style="margin: 0 0 12px; color: #909399">
        对方联系方式：{{ detail?.otherContactInfo || '对方未留联系方式' }}
      </p>
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
