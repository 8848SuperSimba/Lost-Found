<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PostForm from '../../components/PostForm.vue'
import { getPostDetail, updatePost, uploadImage } from '../../api/post'
import { toDateTimeParam } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const existingImageUrls = ref([])

const form = reactive({
  title: '',
  category: '',
  description: '',
  lostFoundTime: null,
  areaCode: '',
  areaText: '',
  locationText: '',
  contactInfo: '',
  reward: '',
  keywords: [],
  localImages: [],
})

const loadDetail = async () => {
  const detail = await getPostDetail(route.params.id)
  form.title = detail.title || ''
  form.category = detail.category || ''
  form.description = detail.description || ''
  form.lostFoundTime = detail.lostFoundTime || null
  form.areaCode = detail.areaCode || ''
  form.areaText = detail.areaText || ''
  form.locationText = detail.locationText || ''
  form.contactInfo = detail.contactInfo || ''
  form.reward = detail.reward || ''
  form.keywords = detail.keywords || []
  existingImageUrls.value = detail.imageUrls || []
}

const submit = async () => {
  loading.value = true
  try {
    const files = (form.localImages || []).map((item) => item.raw).filter(Boolean)
    let imageUrls
    if (files.length > 0) {
      // 用户选择了新图片，上传并替换
      imageUrls = []
      for (const file of files) {
        imageUrls.push(await uploadImage(file))
      }
    } else {
      // 用户未选新图片，沿用原有图片（null 表示不更新该字段）
      imageUrls = existingImageUrls.value.length > 0 ? existingImageUrls.value : null
    }
    await updatePost(route.params.id, {
      title: form.title || null,
      category: form.category || null,
      description: form.description || null,
      lostFoundTime: form.lostFoundTime ? toDateTimeParam(form.lostFoundTime) : null,
      areaCode: form.areaCode || null,
      areaText: form.areaText || null,
      locationText: form.locationText || null,
      contactInfo: form.contactInfo || null,
      reward: form.reward || null,
      keywords: form.keywords,
      imageUrls,
    })
    ElMessage.success('更新成功')
    await router.push(`/posts/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">编辑帖子</h2>
      <div v-if="existingImageUrls.length > 0" style="margin-bottom: 16px">
        <div style="margin-bottom: 8px">当前图片</div>
        <el-space wrap>
          <el-image
            v-for="(img, idx) in existingImageUrls"
            :key="idx"
            :src="img"
            style="width: 100px; height: 100px; border-radius: 4px"
            fit="cover"
            :preview-src-list="existingImageUrls"
          />
        </el-space>
        <div style="margin-top: 4px; color: #909399; font-size: 12px">
          如需更换图片，请在下方重新上传（上传后将替换全部现有图片）
        </div>
      </div>
      <PostForm v-model="form" :include-post-type="false" />
      <el-button type="primary" :loading="loading" @click="submit">保存修改</el-button>
    </div>
  </div>
</template>
