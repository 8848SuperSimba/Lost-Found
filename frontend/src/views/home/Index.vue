<script setup>
import { onMounted, ref } from 'vue'
import { listPosts } from '../../api/post'
import { formatDateTime } from '../../utils/format'

const latestPosts = ref([])

onMounted(async () => {
  const page = await listPosts({ page: 1, size: 5 })
  latestPosts.value = page.records || []
})
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">校园失物招领智能匹配系统</h2>
      <p>快速发布失物/寻物信息，自动匹配并发起联系。</p>
    </div>

    <div class="card-block">
      <h3 style="margin-top: 0">最新发布</h3>
      <el-empty v-if="latestPosts.length === 0" description="暂无数据" />
      <el-table v-else :data="latestPosts" stripe>
        <el-table-column label="标题" prop="title" min-width="220" />
        <el-table-column label="类型" prop="postType" width="100" />
        <el-table-column label="分类" prop="category" width="140" />
        <el-table-column label="区域" prop="areaText" width="140" />
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
