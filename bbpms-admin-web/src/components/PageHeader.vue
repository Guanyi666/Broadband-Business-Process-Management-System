<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

interface Props {
  title: string
  description?: string
  icon?: string
  /** 面包屑导航（如 ['订单', '列表']） */
  breadcrumb?: string[]
  /** 是否显示返回按钮（默认 false） */
  back?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  back: false
})

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<template>
  <div class="page-header">
    <div class="page-header-main">
      <el-breadcrumb v-if="props.breadcrumb?.length" separator="/" class="page-header-breadcrumb">
        <el-breadcrumb-item v-for="b in props.breadcrumb" :key="b">{{ b }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-header-title-row">
        <el-button v-if="props.back" link class="page-header-back" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2 class="page-header-title">{{ title }}</h2>
      </div>
      <p v-if="description" class="page-header-desc">{{ description }}</p>
    </div>
    <div class="page-header-extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  background: var(--el-bg-color);
  border-radius: $radius-base;
  padding: 16px 20px;
  box-shadow: $shadow-card;
  &-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
  &-breadcrumb {
    margin-bottom: 8px;
    font-size: 12px;
  }
  &-title-row {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  &-back {
    font-size: 16px;
    padding: 4px 6px;
  }
  &-desc {
    margin: 4px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}
</style>