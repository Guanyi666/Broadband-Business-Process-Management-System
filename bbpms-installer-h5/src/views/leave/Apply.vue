<template>
  <div class="apply-page">
    <van-nav-bar title="申请请假" :border="false" left-arrow @click-left="$router.back()" fixed placeholder />

    <van-steps :active="step" active-color="#1989fa">
      <van-step>类型</van-step>
      <van-step>时间</van-step>
      <van-step>原因</van-step>
      <van-step>提交</van-step>
    </van-steps>

    <div class="step-body" v-if="step === 0">
      <van-radio-group v-model="form.leaveType" direction="vertical">
        <van-cell-group inset>
          <van-cell clickable @click="form.leaveType = 'CASUAL'">
            <template #title><van-radio name="CASUAL">事假 (CASUAL)</van-radio></template>
          </van-cell>
          <van-cell clickable @click="form.leaveType = 'ANNUAL'">
            <template #title><van-radio name="ANNUAL">年假 (ANNUAL)</van-radio></template>
          </van-cell>
          <van-cell clickable @click="form.leaveType = 'SICK'">
            <template #title><van-radio name="SICK">病假 (SICK) - 需附件</van-radio></template>
          </van-cell>
          <van-cell clickable @click="form.leaveType = 'COMPASSIONATE'">
            <template #title><van-radio name="COMPASSIONATE">丧假 (COMPASSIONATE)</van-radio></template>
          </van-cell>
          <van-cell clickable @click="form.leaveType = 'UNPAID'">
            <template #title><van-radio name="UNPAID">无薪假 (UNPAID)</van-radio></template>
          </van-cell>
        </van-cell-group>
      </van-radio-group>
    </div>

    <div class="step-body" v-else-if="step === 1">
      <van-cell-group inset>
        <van-cell title="开始时间" :value="form.startAt || '点击选择'" is-link @click="pickStart" />
        <van-cell title="结束时间" :value="form.endAt || '点击选择'" is-link @click="pickEnd" />
        <van-cell v-if="totalHours > 0" title="预计时长" :value="`${totalHours} 小时`" />
      </van-cell-group>
      <van-popup v-model:show="showStartPicker" position="bottom">
        <van-date-picker v-model="startDate" :title="'选择开始时间'" @confirm="onStartConfirm" />
      </van-popup>
      <van-popup v-model:show="showEndPicker" position="bottom">
        <van-date-picker v-model="endDate" :title="'选择结束时间'" @confirm="onEndConfirm" />
      </van-popup>
    </div>

    <div class="step-body" v-else-if="step === 2">
      <van-cell-group inset>
        <van-field v-model="form.reason" type="textarea" rows="4" autosize label="原因" placeholder="请详细说明请假原因" />
        <van-field v-if="form.leaveType === 'SICK'" label="附件 URL" placeholder="医疗证明 / 诊断书（URL）" v-model="form.attachmentUrl" />
      </van-cell-group>
    </div>

    <div class="step-body" v-else>
      <van-cell-group inset>
        <van-cell title="类型" :value="form.leaveType" />
        <van-cell title="时间" :value="`${form.startAt} ~ ${form.endAt}`" />
        <van-cell title="时长" :value="`${totalHours} 小时`" />
        <van-cell title="原因" :value="form.reason" />
        <van-cell v-if="requiredLevel > 1" title="审批级别">
          <template #value>
            <van-tag type="warning" round>{{ requiredLevel }} 级审批 (会自动升级)</van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <div class="footer">
      <van-button v-if="step > 0" plain block round @click="step--" style="margin-bottom: 8px">上一步</van-button>
      <van-button v-if="step < 3" type="primary" block round :disabled="!canNext" @click="step++">下一步</van-button>
      <van-button v-else type="success" block round :loading="busy" @click="onSubmit">提交申请</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showToast } from 'vant'
import { applyLeave, type LeaveType } from '@/api/leave'

defineOptions({ name: 'LeaveApply' })

const router = useRouter()
const step = ref(0)
const busy = ref(false)

const form = reactive({
  leaveType: 'CASUAL' as LeaveType,
  startAt: '',
  endAt: '',
  reason: '',
  attachmentUrl: ''
})

const showStartPicker = ref(false)
const showEndPicker = ref(false)
const startDate = ref<string[]>([])
const endDate = ref<string[]>([])

const totalHours = computed(() => {
  if (!form.startAt || !form.endAt) return 0
  const s = new Date(form.startAt).getTime()
  const e = new Date(form.endAt).getTime()
  if (e <= s) return 0
  return Math.round(((e - s) / 3600000) * 100) / 100
})

// Mirror server rule for client-side hint
const requiredLevel = computed(() => {
  if (form.leaveType === 'SICK' || form.leaveType === 'COMPASSIONATE') return 2
  if (totalHours.value >= 16) return 2
  return 1
})

const canNext = computed(() => {
  if (step.value === 0) return !!form.leaveType
  if (step.value === 1) return totalHours.value > 0
  if (step.value === 2) {
    if (!form.reason || form.reason.trim().length < 4) return false
    if (form.leaveType === 'SICK' && !form.attachmentUrl) return false
    return true
  }
  return true
})

function pad(n: number) { return n < 10 ? '0' + n : '' + n }

function pickStart() {
  showStartPicker.value = true
}
function pickEnd() {
  showEndPicker.value = true
}
function onStartConfirm({ selectedValues }: any) {
  form.startAt = `${selectedValues[0]}-${selectedValues[1]}-${selectedValues[2]}T09:00:00`
  showStartPicker.value = false
  if (!form.endAt) {
    form.endAt = form.startAt
  }
}
function onEndConfirm({ selectedValues }: any) {
  form.endAt = `${selectedValues[0]}-${selectedValues[1]}-${selectedValues[2]}T18:00:00`
  showEndPicker.value = false
}

async function onSubmit() {
  busy.value = true
  try {
    const result = await applyLeave({
      leaveType: form.leaveType,
      startAt: form.startAt,
      endAt: form.endAt,
      reason: form.reason,
      attachmentUrl: form.attachmentUrl || undefined
    })
    showSuccessToast('申请已提交')
    setTimeout(() => router.replace('/leave/my'), 600)
  } catch (e: any) {
    showFailToast(e?.message || '提交失败')
  } finally {
    busy.value = false
  }
}
</script>

<style lang="scss" scoped>
.apply-page { min-height: 100vh; background: var(--bbpms-bg); padding-bottom: 100px; }
.step-body { padding-top: 12px; }
.footer { padding: 16px 12px; position: fixed; bottom: 0; left: 0; right: 0; background: #fff; box-shadow: 0 -2px 8px rgba(0,0,0,.04); }
</style>
