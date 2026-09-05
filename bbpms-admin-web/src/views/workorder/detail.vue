<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkorderDetail, completeWorkorder, cancelWorkorder, reassignWorkOrder } from '@/api/workorder'
import { getWorkorderTrack } from '@/api/track'
import type { OrderTrack, TrackEvent } from '@/types/track'
import { dispatchCandidates } from '@/api/dispatch'
import type { DispatchCandidate, WorkorderDetail } from '@/types/order'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'
import OrderTimeline from '@/components/OrderTimeline.vue'
import DispatchDecisionPanel from '@/components/DispatchDecisionPanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const detail = ref<WorkorderDetail | null>(null)
const track = ref<OrderTrack | null>(null)

/** 后端 /track 未就绪时，用已有 timeline 兜底为右轨事件 */
const legacyEvents = computed<TrackEvent[]>(() =>
  (detail.value?.timeline ?? []).map((t: any) => ({
    time: t.createTime || null,
    title: t.toStatusDesc || t.toStatus || '状态更新',
    desc: t.fromStatusDesc && t.toStatusDesc ? `${t.fromStatusDesc} → ${t.toStatusDesc}` : (t.toStatusDesc || ''),
    operatorName: t.operatorName || null,
    operatorRole: t.operatorRole || null,
    source: 'WORKORDER',
    isAuto: t.operatorRole === 'SYSTEM',
    remark: t.remark || null
  }))
)

async function loadTrack() {
  const id = route.params.id as string
  try {
    track.value = await getWorkorderTrack(id)
  } catch {
    track.value = null
  }
}

async function fetchData() {
  loading.value = true
  try {
    detail.value = await getWorkorderDetail(route.params.id as string)
    await loadTrack()
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

async function openReassign() {
  reassignForm.value = { newInstallerId: null, reason: '' }
  candidates.value = []
  reassignVisible.value = true
  reassignLoading.value = true
  try {
    // 复用派单候选接口：仅返回满足接单条件的装维（在岗/可接单/技能匹配/负载达标）
    candidates.value = await dispatchCandidates(detail.value.orderId, 20, detail.value.installerId)
    reassignForm.value.newInstallerId = candidates.value.length ? Number(candidates.value[0].installerId) : null
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

    <OrderTimeline
      :stages="track?.stages"
      :events="track ? track.events : legacyEvents"
      :summary="track?.summary ?? null"
      title="工单状态与人员操作双轨时间线"
      :current-status="detail?.status || ''"
    />

    <!-- 改派弹窗（只展示满足接单条件的装维人员） -->
    <el-dialog v-model="reassignVisible" title="改派工单 · 决策透视" width="820px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="候选装维人员" required>
          <DispatchDecisionPanel
            :candidates="candidates"
            :selected-id="reassignForm.newInstallerId"
            :loading="reassignLoading"
            @select="(candidate) => reassignForm.newInstallerId = Number(candidate.installerId)"
          />
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
