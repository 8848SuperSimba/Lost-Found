<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { triggerAllMatches } from '../../api/match'

const loading = ref(false)

const trigger = async () => {
  loading.value = true
  try {
    const message = await triggerAllMatches()
    ElMessage.success(message || '任务已提交')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">管理员 - 匹配任务</h2>
      <p>点击后将异步提交全量重匹配任务。</p>
      <el-button type="primary" :loading="loading" @click="trigger">触发全量匹配</el-button>
    </div>
  </div>
</template>
