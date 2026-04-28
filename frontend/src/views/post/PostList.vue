<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts } from '../../api/post'
import { CATEGORY_OPTIONS, POST_TYPE_LABEL, POST_TYPE_OPTIONS, STATUS_LABEL } from '../../utils/dict'
import { formatDateTime, toDateTimeParam } from '../../utils/format'

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const posts = ref([])

const query = reactive({
  postType: '',
  category: '',
  areaCode: '',
  status: '',
  keyword: '',
  range: [],
  page: 1,
  size: 10,
})

const fetchData = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = query.range || []
    const pageData = await listPosts({
      postType: query.postType || undefined,
      category: query.category || undefined,
      areaCode: query.areaCode || undefined,
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      startTime: toDateTimeParam(startTime),
      endTime: toDateTimeParam(endTime),
      page: query.page,
      size: query.size,
    })
    posts.value = pageData.records || []
    total.value = pageData.total || 0
  } finally {
    loading.value = false
  }
}

let debounceTimer = null
watch(
  () => [query.postType, query.category, query.areaCode, query.status, query.keyword, query.range],
  () => {
    query.page = 1
    clearTimeout(debounceTimer)
    debounceTimer = setTimeout(fetchData, 500)
  },
  { deep: true },
)

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <div class="toolbar">
        <el-select v-model="query.postType" placeholder="类型" style="width: 120px">
          <el-option v-for="item in POST_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.category" placeholder="分类" clearable style="width: 150px">
          <el-option v-for="item in CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="query.areaCode" placeholder="区域编码（如 A01）" style="width: 160px" clearable />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="进行中" value="OPEN" />
          <el-option label="已匹配" value="MATCHED" />
          <el-option label="已找回" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-date-picker v-model="query.range" type="datetimerange" start-placeholder="开始时间" end-placeholder="结束时间" />
        <el-input v-model="query.keyword" placeholder="搜索标题关键词" style="width: 220px" clearable />
        <el-button type="primary" @click="fetchData">刷新</el-button>
        <el-button type="success" @click="router.push('/posts/create')">发布信息</el-button>
      </div>

      <el-table :data="posts" v-loading="loading">
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" @click="router.push(`/posts/${row.id}`)">{{ row.title }}</el-link>
            <div style="color: #909399; margin-top: 4px">{{ row.descriptionSummary || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ POST_TYPE_LABEL[row.postType] || row.postType }}</template>
        </el-table-column>
        <el-table-column label="分类" prop="category" width="140" />
        <el-table-column label="区域" prop="areaText" width="140" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : row.status === 'RESOLVED' ? 'info' : 'warning'">
              {{ STATUS_LABEL[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>
