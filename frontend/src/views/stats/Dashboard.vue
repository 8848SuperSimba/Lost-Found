<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAreaStats, getCategoryStats, getOverview, getTrendStats } from '../../api/stats'

const loading = reactive({
  overview: false,
  category: false,
  area: false,
  trend: false,
})

const overview = ref(null)
const categoryList = ref([])
const areaList = ref([])
const trendList = ref([])

const filter = reactive({
  days: 30,
  postType: '',
})

const categoryRef = ref()
const areaRef = ref()
const trendRef = ref()
let categoryChart = null
let areaChart = null
let trendChart = null

const fetchOverview = async () => {
  loading.overview = true
  try {
    overview.value = await getOverview()
  } finally {
    loading.overview = false
  }
}

const fetchCharts = async () => {
  loading.category = true
  loading.area = true
  loading.trend = true
  try {
    const [category, area, trend] = await Promise.all([
      getCategoryStats({ days: filter.days, postType: filter.postType || undefined }),
      getAreaStats({ days: filter.days, postType: filter.postType || undefined }),
      getTrendStats({ days: filter.days, postType: filter.postType || undefined }),
    ])
    categoryList.value = category
    areaList.value = area
    trendList.value = trend
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('统计数据加载失败，请刷新重试')
  } finally {
    loading.category = false
    loading.area = false
    loading.trend = false
  }
}

const renderCharts = () => {
  if (categoryRef.value) {
    categoryChart ||= echarts.init(categoryRef.value)
    categoryChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['30%', '60%'],
          data: categoryList.value.map((item) => ({ name: item.categoryDesc, value: item.count })),
        },
      ],
    })
  }
  if (areaRef.value) {
    areaChart ||= echarts.init(areaRef.value)
    areaChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: areaList.value.map((item) => item.areaText || item.areaCode) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: areaList.value.map((item) => item.count), itemStyle: { color: '#409eff' } }],
    })
  }
  if (trendRef.value) {
    trendChart ||= echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendList.value.map((item) => item.date) },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: trendList.value.map((item) => item.count), itemStyle: { color: '#67c23a' } }],
    })
  }
}

watch(() => [filter.days, filter.postType], fetchCharts)

onMounted(async () => {
  await fetchOverview()
  await fetchCharts()
  window.addEventListener('resize', renderCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderCharts)
  categoryChart?.dispose()
  areaChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">数据统计</h2>
    <el-row :gutter="16">
      <el-col :md="4" :sm="12" :xs="12">
        <div class="card-block"><div>失物总数</div><h3>{{ overview?.lostCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="4" :sm="12" :xs="12">
        <div class="card-block"><div>寻物总数</div><h3>{{ overview?.foundCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="4" :sm="12" :xs="12">
        <div class="card-block"><div>已找回</div><h3>{{ overview?.resolvedCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="4" :sm="12" :xs="12">
        <div class="card-block"><div>找回率</div><h3 style="color: #67c23a">{{ overview?.resolvedRate ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="4" :sm="12" :xs="12">
        <div class="card-block"><div>今日新增</div><h3>{{ overview?.todayCount ?? '-' }}</h3></div>
      </el-col>
      <el-col :md="4" :sm="12" :xs="12">
        <div class="card-block"><div>进行中</div><h3>{{ overview?.openCount ?? '-' }}</h3></div>
      </el-col>
    </el-row>

    <div class="card-block">
      <div class="toolbar">
        <el-radio-group v-model="filter.days">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
          <el-radio-button :value="90">近90天</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="filter.postType">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="LOST">失物</el-radio-button>
          <el-radio-button value="FOUND">寻物</el-radio-button>
        </el-radio-group>
      </div>
      <el-row :gutter="16">
        <el-col :md="12" :sm="24" style="min-height: 340px">
          <el-skeleton :loading="loading.category" animated>
            <div ref="categoryRef" style="height: 320px" />
          </el-skeleton>
        </el-col>
        <el-col :md="12" :sm="24" style="min-height: 340px">
          <el-skeleton :loading="loading.area" animated>
            <div ref="areaRef" style="height: 320px" />
          </el-skeleton>
        </el-col>
      </el-row>
      <el-skeleton :loading="loading.trend" animated>
        <div ref="trendRef" style="height: 360px; margin-top: 16px" />
      </el-skeleton>
    </div>
  </div>
</template>
