<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

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
      </el-form>
      <div style="margin-top: 12px; text-align: right">
        没有账号？<el-link type="primary" @click="router.push('/register')">去注册</el-link>
      </div>
    </el-card>
  </div>
</template>
