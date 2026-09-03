<script setup lang="ts">
/**
 * 面板容器
 *
 * 为每个内容区块统一提供 标题 / 副标题 / 操作区 外壳，
 * 并按「error > loading > empty > default」优先级渲染状态层，
 * 失败时显示「加载失败 + 错误信息 + 重试」，绝不显示随机或硬编码数据。
 */
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'

interface Props {
  title: string
  subtitle?: string
  loading?: boolean
  error?: string | null
  empty?: boolean
  emptyText?: string
  retryText?: string
}

const props = withDefaults(defineProps<Props>(), {
  subtitle: '',
  loading: false,
  error: null,
  empty: false,
  emptyText: '暂无数据',
  retryText: '重试'
})

const emit = defineEmits<{ (e: 'retry'): void }>()

type PanelState = 'error' | 'loading' | 'empty' | 'default'

const state = computed<PanelState>(() => {
  if (props.error) return 'error'
  if (props.loading) return 'loading'
  if (props.empty) return 'empty'
  return 'default'
})
</script>

<template>
  <section class="bbpms-panel">
    <header class="bbpms-panel__head">
      <div class="bbpms-panel__titles">
        <h3 class="bbpms-panel__title">{{ title }}</h3>
        <span v-if="subtitle" class="bbpms-panel__subtitle">{{ subtitle }}</span>
      </div>
      <div class="bbpms-panel__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="bbpms-panel__body">
      <div v-if="state === 'error'" class="bbpms-panel__state">
        <el-icon :size="32" color="#f56c6c"><WarningFilled /></el-icon>
        <p class="bbpms-panel__state-title">加载失败</p>
        <p class="bbpms-panel__state-desc">{{ error }}</p>
        <el-button type="primary" size="small" @click="emit('retry')">{{ retryText }}</el-button>
      </div>

      <div v-else-if="state === 'loading'" class="bbpms-panel__state" aria-busy="true">
        <div class="bbpms-panel__skeleton">
          <span
            v-for="i in 4"
            :key="i"
            class="bbpms-panel__skeleton-line"
            :style="{ width: `${100 - (i - 1) * 18}%` }"
          />
        </div>
      </div>

      <div v-else-if="state === 'empty'" class="bbpms-panel__state">
        <p class="bbpms-panel__state-desc">{{ emptyText }}</p>
      </div>

      <slot v-else />
    </div>
  </section>
</template>

<style scoped lang="scss">
.bbpms-panel {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 8px;
  min-width: 0; // 防 grid 子项被长内容撑破

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__titles {
    display: flex;
    align-items: baseline;
    gap: 8px;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
    white-space: nowrap;
  }

  &__subtitle {
    font-size: 12px;
    color: var(--el-text-color-secondary, #909399);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__body {
    flex: 1;
    min-height: 0;
  }

  &__state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 180px;
    text-align: center;
  }

  &__state-title {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary, #303133);
  }

  &__state-desc {
    margin: 0;
    max-width: 90%;
    font-size: 12px;
    color: var(--el-text-color-secondary, #909399);
    word-break: break-all;
  }

  &__skeleton {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 80%;
    margin: 0 auto;
  }

  &__skeleton-line {
    display: block;
    height: 14px;
    border-radius: 4px;
    background: linear-gradient(90deg, #f0f2f5 25%, #e6e8eb 37%, #f0f2f5 63%);
    background-size: 400% 100%;
    animation: panel-shimmer 1.2s ease infinite;
  }
}

@keyframes panel-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
