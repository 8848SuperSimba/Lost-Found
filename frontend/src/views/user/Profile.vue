<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { changePassword, getMe, updateMe } from '../../api/user'
import { formatDateTime } from '../../utils/format'

const authStore = useAuthStore()
const loading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  nickname: '',
  email: '',
  avatarUrl: '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
})

const profileRef = ref()
const pwdRef = ref()

const loadMe = async () => {
  const me = await getMe()
  authStore.user = me
  profileForm.nickname = me.nickname || ''
  profileForm.email = me.email || ''
  profileForm.avatarUrl = me.avatarUrl || ''
}

const saveProfile = async () => {
  await profileRef.value.validate()
  loading.value = true
  try {
    await updateMe(profileForm)
    await authStore.fetchMe()
    ElMessage.success('个人信息已更新')
  } finally {
    loading.value = false
  }
}

const savePassword = async () => {
  await pwdRef.value.validate()
  passwordLoading.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } finally {
    passwordLoading.value = false
  }
}

onMounted(loadMe)
</script>

<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :md="12" :sm="24">
        <div class="card-block">
          <h2 class="page-title">个人资料</h2>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ authStore.user?.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ authStore.user?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ authStore.user?.role || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatDateTime(authStore.user?.createdAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <el-form ref="profileRef" :model="profileForm" label-position="top">
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="头像URL">
              <el-input v-model="profileForm.avatarUrl" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="saveProfile">保存资料</el-button>
          </el-form>
        </div>
      </el-col>
      <el-col :md="12" :sm="24">
        <div class="card-block">
          <h2 class="page-title">修改密码</h2>
          <el-form ref="pwdRef" :model="pwdForm" label-position="top">
            <el-form-item label="旧密码" prop="oldPassword" :rules="[{ required: true, message: '请输入旧密码' }]">
              <el-input v-model="pwdForm.oldPassword" show-password />
            </el-form-item>
            <el-form-item
              label="新密码"
              prop="newPassword"
              :rules="[{ required: true, message: '请输入新密码' }, { min: 6, message: '新密码至少6位' }]"
            >
              <el-input v-model="pwdForm.newPassword" show-password />
            </el-form-item>
            <el-button type="primary" :loading="passwordLoading" @click="savePassword">修改密码</el-button>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
