<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAuditLogs } from '../../api/audit'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const rows = ref([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ action: '', targetType: '', keyword: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const pageData = await listAuditLogs({
      page: pager.page,
      size: pager.size,
      action: query.action || undefined,
      targetType: query.targetType || undefined,
      keyword: query.keyword || undefined,
    })
    rows.value = pageData.records || []
    pager.total = pageData.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  pager.page = 1
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">管理员 - 审计日志</h2>
      <p style="color: #909399; margin: 0 0 16px; font-size: 13px">
        记录管理员封禁用户、关闭帖子等关键操作，便于追溯与核对。
      </p>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="详情 / 操作 / 对象 / 操作人" style="width: 240px" clearable />
        <el-select v-model="query.action" placeholder="操作类型" style="width: 160px" clearable>
          <el-option label="封禁用户" value="BAN_USER" />
          <el-option label="解封用户" value="UNBAN_USER" />
          <el-option label="关闭帖子" value="CLOSE_POST" />
        </el-select>
        <el-select v-model="query.targetType" placeholder="目标类型" style="width: 140px" clearable>
          <el-option label="用户" value="USER" />
          <el-option label="帖子" value="POST" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="140">
          <template #default="{ row }">
            {{ row.adminNickname || row.adminUsername || ('ID ' + row.adminUserId) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" prop="action" width="130" />
        <el-table-column label="目标类型" prop="targetType" width="110" />
        <el-table-column label="目标ID" prop="targetId" width="100">
          <template #default="{ row }">{{ row.targetId != null ? row.targetId : '-' }}</template>
        </el-table-column>
        <el-table-column label="详情" prop="detail" min-width="220" show-overflow-tooltip />
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="
            () => {
              pager.page = 1
              fetchData()
            }
          "
        />
      </div>
    </div>
  </div>
</template>
