<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-code">404</div>
    <div class="error-msg">抱歉，您访问的页面不存在</div>
    <el-button type="primary" @click="router.push('/dashboard')">返回数据看板</el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #fff;
  padding: 80px 20px;
  .error-code {
    font-size: 80px;
    font-weight: 700;
    color: #909399;
  }
  .error-msg {
    color: #606266;
    font-size: 16px;
  }
}
</style>