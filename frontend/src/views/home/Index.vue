<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts } from '../../api/post'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const latestPosts = ref([])
const keyword = ref('')
const qrcodeFailed = ref(false)
/** 站点根路径下的公众号二维码（文件名 wechat-qrcode.png） */
const qrcodeSrc = '/' + 'wechat-qrcode.png'

const goSearch = async () => {
  const query = keyword.value.trim()
  await router.push({
    path: '/posts',
    query: query ? { keyword: query } : {},
  })
}

onMounted(async () => {
  const page = await listPosts({ page: 1, size: 5 })
  latestPosts.value = page.records || []
})
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">BISTU失物招领处</h2>
      <p>快速发布失物/寻物信息，自动匹配并发起联系。</p>
      <div class="toolbar" style="margin-top: 12px">
        <el-input v-model="keyword" placeholder="请输入关键词（如 黑色双肩包 / AirPods）" clearable @keyup.enter="goSearch" />
        <el-button type="primary" @click="goSearch">搜索帖子</el-button>
      </div>
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
        微信扫描下方二维码关注公众号，<br />
        发送「查询」即可查看您最新的匹配结果，发送「我的帖子」查看帖子状态。
      </p>
      <div style="margin: 0 auto; width: 200px">
        <img
          v-show="!qrcodeFailed"
          :src="qrcodeSrc"
          alt="公众号二维码"
          width="200"
          height="200"
          style="display: block; border-radius: 8px; border: 1px solid #ebeef5; object-fit: contain; background: #fff"
          @error="qrcodeFailed = true"
        />
        <div
          v-if="qrcodeFailed"
          style="
            width: 200px;
            height: 200px;
            margin: 0 auto;
            background: #f5f7fa;
            border: 1px dashed #dcdfe6;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 8px;
            color: #909399;
            font-size: 13px;
            text-align: center;
            padding: 12px;
            box-sizing: border-box;
          "
        >
          二维码暂无法显示<br />
          可在微信中搜索「BISTU失物招领处」关注公众号
        </div>
      </div>
    </div>
  </div>
</template>
