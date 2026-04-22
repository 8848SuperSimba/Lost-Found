<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { closeAdminPost, listAdminPosts } from '../../api/post'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const rows = ref([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ postType: '', category: '', status: '', keyword: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const pageData = await listAdminPosts({
      page: pager.page,
      size: pager.size,
      postType: query.postType || undefined,
      category: query.category || undefined,
      status: query.status || undefined,
      keyword: query.keyword || undefined,
    })
    rows.value = pageData.records || []
    pager.total = pageData.total || 0
  } finally {
    loading.value = false
  }
}

const closePost = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入关闭原因', '关闭帖子', { inputPattern: /.+/, inputErrorMessage: '原因不能为空' })
  await closeAdminPost(row.id, value)
  await fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">管理员 - 帖子管理</h2>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="标题关键词" style="width: 220px" />
        <el-select v-model="query.postType" placeholder="类型" clearable style="width: 120px">
          <el-option label="LOST" value="LOST" />
          <el-option label="FOUND" value="FOUND" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="OPEN" value="OPEN" />
          <el-option label="MATCHED" value="MATCHED" />
          <el-option label="RESOLVED" value="RESOLVED" />
          <el-option label="CLOSED" value="CLOSED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="ID" prop="id" width="90" />
        <el-table-column label="标题" prop="title" min-width="220" />
        <el-table-column label="类型" prop="postType" width="100" />
        <el-table-column label="分类" prop="category" width="140" />
        <el-table-column label="状态" prop="status" width="120" />
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button text type="danger" @click="closePost(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
