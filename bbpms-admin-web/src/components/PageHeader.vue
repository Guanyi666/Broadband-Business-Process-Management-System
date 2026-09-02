<script setup lang="ts">
interface Props {
  title: string
  description?: string
  icon?: string
}

defineProps<Props>()
</script>

<template>
  <div class="page-header">
    <div class="page-header-main">
      <h2 class="page-header-title">{{ title }}</h2>
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
  background: #fff;
  border-radius: $radius-base;
  padding: 16px 20px;
  box-shadow: $shadow-card;
  &-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
  &-desc {
    margin: 4px 0 0;
    color: #909399;
    font-size: 13px;
  }
}
</style>