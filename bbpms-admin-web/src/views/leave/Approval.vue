<template>
  <div class="leave-approval">
    <PageHeader title="请假审批" :breadcrumb="['请假', '审批']" />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="待我审批" name="pending">
        <BBPMSTable :data="pending" :columns="columns" :loading="loading">
          <template #action="{ row }">
            <PermissionButton v-hasPermi="`leave:approve-l${row.currentLevel + 1}`" type="primary" size="small" @click="openApprove(row, row.currentLevel + 1)">审批</PermissionButton>
          </template>
        </BBPMSTable>
      </el-tab-pane>
      <el-tab-pane label="团队日历" name="calendar">
        <div class="cal-hdr">
          <el-date-picker v-model="calRange" type="datetimerange" range-separator="至" />
          <el-button @click="loadCalendar" type="primary">查询</el-button>
        </div>
        <BBPMSTable :data="calendar" :columns="calColumns" :loading="loading" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="approveVisible" title="审批" width="500">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="请假类型">{{ current?.leaveType }}</el-form-item>
        <el-form-item label="时长">{{ current?.totalHours }} 小时</el-form-item>
        <el-form-item label="原因">{{ current?.reason }}</el-form-item>
        <el-form-item label="审批动作">
          <el-radio-group v-model="approveForm.action">
            <el-radio value="APPROVE">同意</el-radio>
            <el-radio value="REJECT">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="approveForm.comment" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPendingApprovals, approveLeave, fetchTeamCalendar, type LeaveVO } from '@/api/leave'
import BBPMSTable from '@/components/BBPMSTable.vue'
import PermissionButton from '@/components/PermissionButton.vue'
import PageHeader from '@/components/PageHeader.vue'

defineOptions({ name: 'LeaveApproval' })

const activeTab = ref('pending')
const pending = ref<LeaveVO[]>([])
const calendar = ref<LeaveVO[]>([])
const loading = ref(false)
const calRange = ref<[string, string] | null>(null)

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'applicantName', label: '申请人' },
  { prop: 'leaveType', label: '类型', width: 100 },
  { prop: 'startAt', label: '开始', width: 160, formatter: (r: any) => formatTime(r.startAt) },
  { prop: 'endAt', label: '结束', width: 160, formatter: (r: any) => formatTime(r.endAt) },
  { prop: 'totalHours', label: '小时', width: 80 },
  { prop: 'reason', label: '原因' },
  { prop: 'requiredLevel', label: '级别', width: 80, formatter: (r: any) => `L${r.requiredLevel}` },
  { prop: 'currentLevel', label: '当前', width: 80, formatter: (r: any) => `L${r.currentLevel}` }
]

const calColumns = [
  { prop: 'applicantName', label: '申请人' },
  { prop: 'leaveType', label: '类型' },
  { prop: 'startAt', label: '开始', formatter: (r: any) => formatTime(r.startAt) },
  { prop: 'endAt', label: '结束', formatter: (r: any) => formatTime(r.endAt) },
  { prop: 'totalHours', label: '小时' },
  { prop: 'status', label: '状态' }
]

const approveVisible = ref(false)
const current = ref<LeaveVO | null>(null)
const busy = ref(false)
const approveForm = ref<{ action: 'APPROVE' | 'REJECT'; comment: string; approvalLevel: number }>({
  action: 'APPROVE', comment: '', approvalLevel: 1
})

function formatTime(iso?: string) {
  if (!iso) return '--'
  return iso.substring(0, 16).replace('T', ' ')
}

async function loadPending() {
  loading.value = true
  try { pending.value = await fetchPendingApprovals() } catch { /* noop */ } finally { loading.value = false }
}

async function loadCalendar() {
  if (!calRange.value) return
  loading.value = true
  try {
    calendar.value = await fetchTeamCalendar(calRange.value[0], calRange.value[1])
  } finally { loading.value = false }
}

function openApprove(row: LeaveVO, level: number) {
  current.value = row
  approveForm.value = { action: 'APPROVE', comment: '', approvalLevel: level }
  approveVisible.value = true
}

async function onSubmit() {
  if (!current.value) return
  busy.value = true
  try {
    await approveLeave(current.value.id, {
      approvalLevel: approveForm.value.approvalLevel,
      action: approveForm.value.action,
      comment: approveForm.value.comment
    })
    ElMessage.success('已提交')
    approveVisible.value = false
    await loadPending()
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally { busy.value = false }
}

watch(activeTab, (t) => { if (t === 'pending') loadPending() })
onMounted(loadPending)
</script>

<style lang="scss" scoped>
.leave-approval { padding: 16px; }
.cal-hdr { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
