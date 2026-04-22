<script setup>
import { ref } from 'vue'
import { CATEGORY_OPTIONS } from '../utils/dict'

const props = defineProps({
  modelValue: { type: Object, required: true },
  includePostType: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue'])
const keywordInput = ref('')

const update = (key, value) => {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

const addKeyword = () => {
  const value = keywordInput.value.trim()
  if (!value) return
  const next = [...(props.modelValue.keywords || [])]
  if (next.length >= 10) return
  if (!next.includes(value)) next.push(value)
  update('keywords', next)
  keywordInput.value = ''
}

const removeKeyword = (value) => {
  update(
    'keywords',
    (props.modelValue.keywords || []).filter((item) => item !== value),
  )
}

const onImageChange = (files) => {
  update('localImages', files)
}
</script>

<template>
  <el-form label-position="top">
    <el-row :gutter="16">
      <el-col :md="12" :sm="24">
        <el-form-item v-if="includePostType" label="帖子类型">
          <el-radio-group :model-value="modelValue.postType" @update:model-value="(v) => update('postType', v)">
            <el-radio-button label="LOST">失物</el-radio-button>
            <el-radio-button label="FOUND">寻物</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :md="12" :sm="24">
        <el-form-item label="分类">
          <el-select :model-value="modelValue.category" @update:model-value="(v) => update('category', v)" style="width: 100%">
            <el-option v-for="item in CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="标题">
      <el-input :model-value="modelValue.title" @update:model-value="(v) => update('title', v)" maxlength="128" show-word-limit />
    </el-form-item>

    <el-form-item label="关键词（最多10个）">
      <el-space wrap>
        <el-tag v-for="tag in modelValue.keywords || []" :key="tag" closable @close="removeKeyword(tag)">{{ tag }}</el-tag>
        <el-input
          v-model="keywordInput"
          style="width: 220px"
          placeholder="输入关键词后回车"
          @keyup.enter="addKeyword"
        />
        <el-button @click="addKeyword">添加</el-button>
      </el-space>
    </el-form-item>

    <el-form-item label="描述">
      <el-input
        type="textarea"
        :rows="5"
        :model-value="modelValue.description"
        @update:model-value="(v) => update('description', v)"
      />
    </el-form-item>

    <el-row :gutter="16">
      <el-col :md="12" :sm="24">
        <el-form-item label="遗失/拾获时间">
          <el-date-picker
            :model-value="modelValue.lostFoundTime"
            type="datetime"
            style="width: 100%"
            @update:model-value="(v) => update('lostFoundTime', v)"
          />
        </el-form-item>
      </el-col>
      <el-col :md="12" :sm="24">
        <el-form-item label="区域编码">
          <el-input :model-value="modelValue.areaCode" @update:model-value="(v) => update('areaCode', v)" placeholder="如 A1" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="区域名称">
      <el-input :model-value="modelValue.areaText" @update:model-value="(v) => update('areaText', v)" placeholder="如 图书馆" />
    </el-form-item>

    <el-form-item label="详细地点">
      <el-input :model-value="modelValue.locationText" @update:model-value="(v) => update('locationText', v)" />
    </el-form-item>

    <el-form-item label="联系方式（可选）">
      <el-input :model-value="modelValue.contactInfo" @update:model-value="(v) => update('contactInfo', v)" />
    </el-form-item>

    <el-form-item label="悬赏（可选）">
      <el-input :model-value="modelValue.reward" @update:model-value="(v) => update('reward', v)" />
    </el-form-item>

    <el-form-item label="图片（最多5张）">
      <el-upload
        list-type="picture-card"
        :auto-upload="false"
        :limit="5"
        :on-change="(_file, files) => onImageChange(files)"
        :on-remove="(_file, files) => onImageChange(files)"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
    </el-form-item>
  </el-form>
</template>
