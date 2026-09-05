<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkorderDetail, completeWorkorder, cancelWorkorder, reassignWorkOrder } from '@/api/workorder'
import { dispatchCandidates } from '@/api/dispatch'
import type { DispatchCandidate } from '@/types/order'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const detail = ref<any>(null)

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getWorkorderDetail(route.params.id as string)
  } finally {
    loading.value = false
  }
}

async function onComplete() {
  try {
    await ElMessageBox.confirm('确认将该工单标记为已完成吗？', '提示', { type: 'success' })
  } catch { return }
  await completeWorkorder(detail.value.id)
  ElMessage.success('已完成')
  fetchData()
}

async function onCancel() {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因：', '取消工单', { inputPattern: /.+/, inputErrorMessage: '取消原因不能为空' })
    await cancelWorkorder(detail.value.id, value)
    ElMessage.success('已取消')
    fetchData()
  } catch {/* canceled */}
}

// ---------- 改派（P1-4）：弹窗 + 可接单装维下拉 ----------
const reassignVisible = ref(false)
const reassignLoading = ref(false)
const candidates = ref<DispatchCandidate[]>([])
const reassignForm = ref<{ newInstallerId: number | null; reason: string }>({ newInstallerId: null, reason: '' })

function candidateLabel(c: DispatchCandidate): string {
  const identity = c.username ? `${c.name}（${c.username}）` : c.name
  const status = c.status === 'AVAILABLE' ? '可接单' : '不可用'
  const parts = [`${identity}｜${status}｜当前工单 ${c.workload ?? 0}｜评分 ${c.rating ?? '-'}`]
  if (c.distanceKm != null) parts.push(`距客户 ${c.distanceKm.toFixed(1)} km`)
  return parts.join('｜')
}

async function openReassign() {
  reassignForm.value = { newInstallerId: null, reason: '' }
  candidates.value = []
  reassignVisible.value = true
  reassignLoading.value = true
  try {
    // 复用派单候选接口：仅返回满足接单条件的装维（在岗/可接单/技能匹配/负载达标）
    candidates.value = await dispatchCandidates(detail.value.orderId, 20, detail.value.installerId)
  } catch {
    ElMessage.error('装维候选加载失败，请重试')
  } finally {
    reassignLoading.value = false
  }
}

async function submitReassign() {
  if (!reassignForm.value.newInstallerId) {
    ElMessage.warning('请选择接单装维人员')
    return
  }
  if (!reassignForm.value.reason.trim()) {
    ElMessage.warning('请填写改派原因')
    return
  }
  reassignLoading.value = true
  try {
    await reassignWorkOrder(detail.value.id, {
      newInstallerId: reassignForm.value.newInstallerId,
      reason: reassignForm.value.reason.trim()
    })
    ElMessage.success('改派成功')
    reassignVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.response?.data?.msg || e?.message || '改派失败，请重试')
  } finally {
    reassignLoading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader :title="`工单 ${detail?.workNo || ''}`">
      <template #extra>
        <el-button type="primary" @click="onComplete" v-if="detail?.status === 'IN_PROGRESS' && auth.hasPermission('workorder:complete')">标记完成</el-button>
        <el-button @click="openReassign" v-if="['DISPATCHED', 'ACCEPTED', 'IN_PROGRESS', 'STALLED'].includes(detail?.status) && auth.hasPermission('workorder:reassign')">改派</el-button>
        <el-button type="danger" plain @click="onCancel" v-if="['PENDING', 'DISPATCHED', 'ACCEPTED'].includes(detail?.status) && auth.hasPermission('workorder:cancel')">取消</el-button>
      </template>
    </PageHeader>

    <div class="app-card">
      <h3>工单信息</h3>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="工单号">{{ detail.workNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <BBPMSStatusTag :status="detail.status" :label="detail.statusDesc || detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="订单 ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="装维人员">{{ detail.installerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="调度员">{{ detail.dispatcherName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户电话">{{ detail.customerPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="套餐">{{ detail.packageName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.installAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="派单时间">{{ formatDate(detail.dispatchTime) }}</el-descriptions-item>
        <el-descriptions-item label="已接单">{{ formatDate(detail.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(detail.finishTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 改派弹窗（只展示满足接单条件的装维人员） -->
    <el-dialog v-model="reassignVisible" title="改派工单" width="560px" :close-on-click-modal="false">
      <el-form label-width="110px" v-loading="reassignLoading">
        <el-form-item label="接单装维人员" required>
          <el-select
            v-model="reassignForm.newInstallerId"
            filterable
            placeholder="请选择可接单的装维人员"
            style="width: 100%"
            :empty-text="candidates.length ? '搜索无结果' : '暂无可接单装维人员'"
          >
            <el-option
              v-for="c in candidates"
              :key="c.installerId"
              :value="c.installerId"
              :label="candidateLabel(c)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="改派原因" required>
          <el-input v-model="reassignForm.reason" type="textarea" :rows="2" placeholder="请填写改派原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">取消</el-button>
        <el-button type="primary" :loading="reassignLoading" @click="submitReassign">确认改派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss"></style>
