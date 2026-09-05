<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const portalUrl = computed(() => import.meta.env.VITE_CUSTOMER_PORTAL_URL || 'http://localhost:9003/')

async function logout() {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <main class="customer-entry">
    <section class="entry-card">
      <div class="logo">B</div>
      <h1>客户自助服务</h1>
      <p>当前账号是客户账号，请前往客户门户办理宽带申请、订单查询、预约和报障。</p>
      <el-button tag="a" :href="portalUrl" type="primary" size="large">进入客户门户</el-button>
      <el-button link @click="logout">退出当前账号</el-button>
      <small>客户门户与管理后台相互隔离，首次进入需要重新登录。</small>
    </section>
  </main>
</template>

<style scoped lang="scss">
.customer-entry {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #eef6ff, #f7fbff 55%, #e8f3ff);
}
.entry-card {
  width: min(460px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 44px 40px;
  text-align: center;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 16px 40px rgba(24, 144, 255, 0.14);
  h1 { margin: 0; color: #303133; }
  p { margin: 0 0 8px; color: #606266; line-height: 1.7; }
  small { color: #909399; }
}
.logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  line-height: 64px;
  background: linear-gradient(135deg, #409eff, #0057b8);
}
</style>
