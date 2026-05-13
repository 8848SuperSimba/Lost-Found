<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, wxLogin } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const wxLoading = ref(false)
const wxAppid = import.meta.env.VITE_WX_APPID || 'wxab9f4f11742eb414'
const isWechatBrowser = computed(() => /micromessenger/i.test(window.navigator.userAgent))

const form = reactive({
  identifier: '',
  password: '',
})

const rules = {
  identifier: [{ required: true, message: '请输入用户名/手机号/邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const formRef = ref()

const onSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    authStore.setAuth(data.token, data.user)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/posts'
    await router.push(String(redirect))
  } finally {
    loading.value = false
  }
}

const getRedirectPath = () => {
  if (typeof route.query.state === 'string' && route.query.state) {
    try {
      return decodeURIComponent(route.query.state)
    } catch {
      // ignore
    }
  }
  return String(route.query.redirect || '/posts')
}

const onWxLogin = () => {
  const redirectPath = String(route.query.redirect || '/posts')
  const redirectUri = encodeURIComponent(`${window.location.origin}/login`)
  const state = encodeURIComponent(redirectPath)
  const url =
    `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${wxAppid}` +
    `&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_base&state=${state}#wechat_redirect`
  window.location.href = url
}

const handleWxCallback = async () => {
  if (typeof route.query.code !== 'string' || !route.query.code) return
  wxLoading.value = true
  try {
    const data = await wxLogin({ code: route.query.code })
    authStore.setAuth(data.token, data.user)
    ElMessage.success('微信登录成功')
    await router.replace(getRedirectPath())
  } finally {
    wxLoading.value = false
  }
}

onMounted(handleWxCallback)
</script>

<template>
  <div class="page-container" style="display: flex; justify-content: center; padding-top: 60px">
    <el-card style="width: 420px">
      <h2 style="margin-top: 0">登录系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号" prop="identifier">
          <el-input v-model="form.identifier" placeholder="用户名/手机号/邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="请输入密码" @keyup.enter="onSubmit" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登录</el-button>
        <el-divider />
        <el-button :loading="wxLoading" style="width: 100%" @click="onWxLogin">微信授权登录</el-button>
        <p style="margin-top: 8px; color: #909399; font-size: 12px">
          {{ isWechatBrowser ? '检测到微信环境，可直接授权登录。' : '建议在微信内打开以完成授权登录。' }}
        </p>
      </el-form>
      <div style="margin-top: 12px; text-align: right">
        没有账号？<el-link type="primary" @click="router.push('/register')">去注册</el-link>
      </div>
    </el-card>
  </div>
</template>
