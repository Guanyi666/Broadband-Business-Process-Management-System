<template>
  <div class="sla-monitor">
    <PageHeader title="SLA 监控" :breadcrumb="['SLA', '监控']" />

    <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
      即将超时工单：{{ expiring.length }} 个；已 STALLED：{{ stalled.length }} 个；已 AUTO_CANCELLED：{{ cancelled.length }} 个
    </el-alert>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="`即将超时 (${expiring.length})`" name="expiring">
        <BBPMSTable :data="expiring" :columns="columns" :loading="loading">
          <template #status="{ row }">
            <BBPMSStatusTag :status="row.status" :type-map="STATUS_MAP" />
          </template>
          <template #action="{ row }">
            <PermissionButton v-hasPermi="'workorder:reassign'" type="primary" size="small" @click="onReassign(row)">改派</PermissionButton>
          </template>
        </BBPMSTable>
      </el-tab-pane>

      <el-tab-pane :label="`已停滞 (${stalled.length})`" name="stalled">
        <BBPMSTable :data="stalled" :columns="columns" :loading="loading">
          <template #status="{ row }">
            <BBPMSStatusTag :status="row.status" :type-map="STATUS_MAP" />
          </template>
          <template #action="{ row }">
            <PermissionButton v-hasPermi="'workorder:resume'" type="success" size="small" @click="onResume(row)">恢复</PermissionButton>
            <PermissionButton v-hasPermi="'workorder:reassign'" type="primary" size="small" @click="onReassign(row)">改派</PermissionButton>
            <PermissionButton v-hasPermi="'workorder:force-close'" type="danger" size="small" @click="onForceClose(row)">强关</PermissionButton>
          </template>
        </BBPMSTable>
      </el-tab-pane>

      <el-tab-pane :label="`已自动取消 (${cancelled.length})`" name="cancelled">
        <BBPMSTable :data="cancelled" :columns="columns" :loading="loading">
          <template #status="{ row }">
            <BBPMSStatusTag :status="row.status" :type-map="STATUS_MAP" />
          </template>
        </BBPMSTable>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="reassignVisible" title="改派工单" width="500">
      <el-form :model="reassignForm" label-width="80px">
        <el-form-item label="工单">{{ current?.workNo }}</el-form-item>
        <el-form-item label="新装维">
          <el-select v-model="reassignForm.newInstallerId" filterable placeholder="选择新装维" style="width:100%">
            <el-option v-for="i in installers" :key="i.userId" :label="i.realName || i.username" :value="i.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="reassignForm.reason" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy" @click="onReassignSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchExpiring, reassignWorkOrder, forceCloseWorkOrder, resumeWorkOrder, pageWorkorders, type WorkOrderVO } from '@/api/workorder'
import { pageInstallers } from '@/api/installer'
import { pageWorkOrders } from '@/api/workorder'
import BBPMSTable from '@/components/BBPMSTable.vue'
import PermissionButton from '@/components/PermissionButton.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

defineOptions({ name: 'SlaMonitor' })

const STATUS_MAP: Record<string, any> = {
  DISPATCHED: 'primary',
  ACCEPTED: 'warning',
  IN_PROGRESS: 'success',
  STALLED: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger',
  CANCELLED: 'info',
  AUTO_CANCELLED: 'danger',
  REASSIGNING: 'primary'
}

const activeTab = ref('expiring')
const loading = ref(false)
const expiring = ref<WorkOrderVO[]>([])
const stalled = ref<WorkOrderVO[]>([])
const cancelled = ref<WorkOrderVO[]>([])
const installers = ref<any[]>([])

const columns = [
  { prop: 'workNo', label: '工单号', width: 160 },
  { prop: 'installerName', label: '装维', width: 100 },
  { prop: 'installAddress', label: '地址' },
  { prop: 'expectedFinishTime', label: '期望完成', width: 180, formatter: (r: any) => formatTime(r.expectedFinishTime) },
  { prop: 'lastActiveAt', label: '最后心跳', width: 180, formatter: (r: any) => formatTime(r.lastActiveAt) },
  { prop: 'stallReason', label: '原因/状态', formatter: (r: any) => r.stallReason || r.cancelType || '--' }
]

async function loadAll() {
  loading.value = true
  try {
    const [exp, stl, cnl] = await Promise.all([
      fetchExpiring(30),
      pageWorkorders({ pageNum: 1, pageSize: 50, status: 'STALLED' as any } as any).then((p: any) => p.list || []),
      pageWorkorders({ pageNum: 1, pageSize: 50, status: 'AUTO_CANCELLED' as any } as any).then((p: any) => p.list || [])
    ])
    expiring.value = exp || []
    stalled.value = stl
    cancelled.value = cnl
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally { loading.value = false }
}

async function loadInstallers() {
  try {
    const p: any = await pageInstallers({ pageNum: 1, pageSize: 100 })
    installers.value = p.list || []
  } catch { /* noop */ }
}

const reassignVisible = ref(false)
const current = ref<WorkOrderVO | null>(null)
const busy = ref(false)
const reassignForm = ref({ newInstallerId: 0, reason: '' })

function onReassign(row: WorkOrderVO) {
  current.value = row
  reassignForm.value = { newInstallerId: 0, reason: '' }
  reassignVisible.value = true
}

async function onReassignSubmit() {
  if (!current.value || !reassignForm.value.newInstallerId) {
    ElMessage.warning('请选择新装维')
    return
  }
  busy.value = true
  try {
    await reassignWorkOrder(current.value.id, reassignForm.value)
    ElMessage.success('已改派')
    reassignVisible.value = false
    await loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '失败')
  } finally { busy.value = false }
}

async function onResume(row: WorkOrderVO) {
  try { await ElMessageBox.confirm(`确认恢复工单 ${row.workNo} ?`, '确认') } catch { return }
  try { await resumeWorkOrder(row.id); ElMessage.success('已恢复'); await loadAll() } catch (e: any) { ElMessage.error(e?.message || '失败') }
}

async function onForceClose(row: WorkOrderVO) {
  try { await ElMessageBox.confirm(`确认强制关闭工单 ${row.workNo}? 此操作不可逆`, '危险操作', { type: 'warning' }) } catch { return }
  try {
    await forceCloseWorkOrder(row.id, { reason: '强制关闭', cancelType: 'ADMIN' })
    ElMessage.success('已关闭')
    await loadAll()
  } catch (e: any) { ElMessage.error(e?.message || '失败') }
}

function formatTime(iso?: string) {
  if (!iso) return '--'
  return iso.substring(0, 19).replace('T', ' ')
}

onMounted(() => { loadAll(); loadInstallers() })
onActivated(() => loadAll())
</script>

<style lang="scss" scoped>
.sla-monitor { padding: 16px; }
</style>
