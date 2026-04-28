<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import PostForm from '../../components/PostForm.vue'
import { createPost, uploadImage } from '../../api/post'
import { toDateTimeParam } from '../../utils/format'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  postType: 'LOST',
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

const submit = async () => {
  if (!form.title || !form.category || !form.lostFoundTime || !form.areaCode) {
    ElMessage.warning('标题、分类、遗失/拾获时间、区域编码为必填项')
    return
  }
  if (dayjs(form.lostFoundTime).isAfter(dayjs())) {
    ElMessage.warning('遗失/拾获时间不能是未来时间')
    return
  }
  loading.value = true
  try {
    const files = (form.localImages || []).map((item) => item.raw).filter(Boolean)
    const imageUrls = []
    for (const file of files) {
      imageUrls.push(await uploadImage(file))
    }

    const postId = await createPost({
      postType: form.postType,
      title: form.title,
      category: form.category,
      description: form.description || null,
      lostFoundTime: toDateTimeParam(form.lostFoundTime),
      areaCode: form.areaCode,
      areaText: form.areaText || null,
      locationText: form.locationText || null,
      contactInfo: form.contactInfo || null,
      reward: form.reward || null,
      keywords: form.keywords,
      imageUrls,
    })
    ElMessage.success('发布成功')
    await router.push(`/posts/${postId}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="card-block">
      <h2 class="page-title">发布失物/寻物信息</h2>
      <PostForm v-model="form" />
      <el-button type="primary" :loading="loading" @click="submit">提交发布</el-button>
    </div>
  </div>
</template>
