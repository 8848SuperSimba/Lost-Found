<script setup>
import { onMounted, ref } from 'vue'
import { getUserStats } from '../../api/stats'

const stats = ref(null)

onMounted(async () => {
  stats.value = await getUserStats()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">管理员 - 用户统计</h2>
    <el-row :gutter="16">
      <el-col :md="6" :sm="12">
        <div class="card-block"><div>用户总数</div><h3>{{ stats?.totalCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="6" :sm="12">
        <div class="card-block"><div>今日新增</div><h3>{{ stats?.todayCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="6" :sm="12">
        <div class="card-block"><div>封禁用户</div><h3>{{ stats?.bannedCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="6" :sm="12">
        <div class="card-block"><div>近7天活跃</div><h3>{{ stats?.activeCount ?? '-' }}</h3></div>
      </el-col>
    </el-row>
  </div>
</template>
