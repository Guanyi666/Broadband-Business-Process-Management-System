<script setup lang="ts" generic="T extends Record<string, any>">
import { ref } from 'vue'

interface Column {
  prop: keyof T | string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: boolean | 'left' | 'right'
  sortable?: boolean
  formatter?: (row: T, column: any, value: any, index: number) => any
  slot?: string
}

interface Props {
  data: T[]
  columns: Column[]
  total?: number
  pageNum?: number
  pageSize?: number
  loading?: boolean
  selection?: boolean
  showPagination?: boolean
  rowKey?: string
}

const props = withDefaults(defineProps<Props>(), {
  total: 0,
  pageNum: 1,
  pageSize: 10,
  loading: false,
  selection: false,
  showPagination: true,
  rowKey: 'id'
})

const emit = defineEmits<{
  (e: 'update:pageNum', v: number): void
  (e: 'update:pageSize', v: number): void
  (e: 'refresh'): void
  (e: 'selection-change', rows: T[]): void
}>()

const selectedRows = ref<T[]>([])

function onSelectionChange(rows: T[]) {
  selectedRows.value = rows
  emit('selection-change', rows)
}

function onPageChange(p: number) {
  emit('update:pageNum', p)
}

function onSizeChange(s: number) {
  emit('update:pageSize', s)
}
</script>

<template>
  <div class="bbpms-table">
    <div v-if="$slots.toolbar" class="table-toolbar">
      <slot name="toolbar" :selected="selectedRows" />
    </div>
    <el-table
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      stripe
      border
      @selection-change="onSelectionChange"
    >
      <el-table-column v-if="selection" type="selection" width="50" />
      <el-table-column
        v-for="col in columns"
        :key="String(col.prop)"
        :prop="col.prop as string"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align || 'left'"
        :fixed="col.fixed"
        :sortable="col.sortable"
        :formatter="col.formatter"
      >
        <template v-if="col.slot" #default="{ row, $index }">
          <slot :name="col.slot" :row="row" :index="$index" />
        </template>
      </el-table-column>
      <el-table-column v-if="$slots.action" label="操作" width="180" fixed="right" align="center">
        <template #default="{ row, $index }">
          <slot name="action" :row="row" :index="$index" />
        </template>
      </el-table-column>
    </el-table>

    <div v-if="showPagination" class="pagination-wrap">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.bbpms-table {
  background: var(--el-bg-color);
  border-radius: $radius-base;
  padding: 16px;
  .table-toolbar {
    margin-bottom: 12px;
  }

  // 列多时小屏允许横向滚动（el-table 自身行内滚动 + 外层兜底）
  :deep(.el-table) {
    .el-table__body-wrapper {
      overflow-x: auto;
    }
  }
}

// —— 响应式：<1024px 分页换行、内边距收紧 ——
@media (max-width: 1023px) {
  .bbpms-table {
    padding: 12px;
    .pagination-wrap {
      margin-top: 12px;
      overflow-x: auto;
      :deep(.el-pagination) {
        flex-wrap: wrap;
        justify-content: center;
      }
    }
  }
}
</style>