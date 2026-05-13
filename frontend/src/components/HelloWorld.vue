<script setup>
import { computed, ref } from 'vue'

defineProps({
  msg: {
    type: String,
    required: true,
  },
})

const count = ref(0)

const doubled = computed(() => count.value * 2)

const increment = () => {
  count.value += 1
}

const reset = () => {
  count.value = 0
}
</script>

<template>
  <div class="hello-world">
    <h1>{{ msg }}</h1>

    <div class="card">
      <p class="hint">点击下方按钮查看计数变化。</p>
      <div class="counter-row">
        <button type="button" class="btn" @click="increment">count + 1</button>
        <button type="button" class="btn ghost" @click="reset">reset</button>
      </div>
      <p class="stats">当前 count：<strong>{{ count }}</strong>；翻倍值：<strong>{{ doubled }}</strong></p>
      <p class="edit-hint">
        编辑 <code>components/HelloWorld.vue</code> 并保存，可验证热更新（HMR）。
      </p>
    </div>

    <div class="links">
      <p>
        官方文档：
        <a href="https://vuejs.org/" target="_blank" rel="noreferrer">Vue 3</a>
        、
        <a href="https://vite.dev/" target="_blank" rel="noreferrer">Vite</a>
      </p>
      <p>
        脚手架说明：
        <a href="https://github.com/vuejs/create-vue" target="_blank" rel="noreferrer">create-vue</a>
      </p>
    </div>

    <p class="read-the-docs">更多用法请参考 Vue 与 Vite 官方文档。</p>
  </div>
</template>

<style scoped>
.hello-world {
  max-width: 640px;
  margin: 0 auto;
  padding: 16px;
  text-align: left;
}

h1 {
  font-weight: 600;
  font-size: 1.75rem;
  margin-bottom: 1rem;
}

.card {
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
}

.hint {
  color: #606266;
  margin: 0 0 12px;
  font-size: 14px;
}

.counter-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.btn {
  border-radius: 8px;
  border: 1px solid transparent;
  padding: 0.55em 1.1em;
  font-size: 0.95em;
  font-weight: 500;
  font-family: inherit;
  background-color: #1a1a1a;
  color: #fff;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.btn:hover {
  background-color: #333;
}

.btn.ghost {
  background-color: #fff;
  color: #1a1a1a;
  border-color: #ccc;
}

.stats {
  margin: 10px 0 0;
  font-size: 14px;
}

.edit-hint {
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
}

code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}

.links {
  margin-top: 18px;
  font-size: 14px;
}

.links a {
  color: #646cff;
}

.read-the-docs {
  margin-top: 18px;
  color: #888;
  font-size: 13px;
}
</style>
