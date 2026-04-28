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

    <div class="card-block" style="text-align: center; padding: 24px">
      <h3 style="margin-top: 0">关注公众号，随时查询匹配结果</h3>
      <p style="color: #606266">
        微信扫描下方二维码关注「校园失物招领」公众号，<br />
        发送「查询」即可查看您最新的匹配结果，发送「我的帖子」查看帖子状态。
      </p>
      <div
        style="
          width: 160px;
          height: 160px;
          margin: 0 auto;
          background: #f5f7fa;
          border: 1px dashed #dcdfe6;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 8px;
          color: #909399;
          font-size: 13px;
        "
      >
        测试号二维码<br />（请替换为实际二维码图片）
      </div>
      <p style="color: #909399; font-size: 12px; margin-top: 8px">注：当前使用微信公众号测试号，需扫码申请关注权限</p>
    </div>
  </div>
</template>
