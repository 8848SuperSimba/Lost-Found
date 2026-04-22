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
}

const submit = async () => {
  loading.value = true
  try {
    const files = (form.localImages || []).map((item) => item.raw).filter(Boolean)
    let imageUrls = null
    if (files.length > 0) {
      imageUrls = []
      for (const file of files) {
        imageUrls.push(await uploadImage(file))
      }
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
      <PostForm v-model="form" :include-post-type="false" />
      <el-button type="primary" :loading="loading" @click="submit">保存修改</el-button>
    </div>
  </div>
</template>
