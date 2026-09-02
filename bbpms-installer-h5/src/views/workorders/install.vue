<template>
  <div class="install-page">
    <van-nav-bar :title="`装机 ${wo?.workNo || ''}`" left-arrow fixed placeholder @click-left="$router.back()" />

    <van-steps :active="currentStep" active-color="#1989fa" class="steps">
      <van-step v-for="s in stepDefs" :key="s.key" :title="s.title" />
    </van-steps>

    <!-- Step 1: 到达现场 -->
    <section v-if="currentStep === 0" class="step-pane">
      <van-cell-group inset>
        <van-cell title="客户地址" :value="wo?.installAddress" />
        <van-cell title="到达时间">
          <template #value>{{ arrivedAt ? '已记录' : '尚未到达' }}</template>
        </van-cell>
      </van-cell-group>
      <div class="pane-body">
        <van-button block plain @click="refreshGps">刷新位置</van-button>
        <van-button type="primary" block round :loading="arriving" :disabled="!myLoc" @click="confirmArrive">
          确认到达现场
        </van-button>
      </div>
    </section>

    <!-- Step 2: 装机信息 -->
    <section v-else-if="currentStep === 1" class="step-pane">
      <van-form @submit="onInfoNext">
        <van-cell-group inset>
          <van-field v-model="info.onuMac" label="ONU MAC" placeholder="AA:BB:CC:DD:EE:FF" />
          <van-field v-model="info.onuSn" label="ONU SN" placeholder="设备序列号" />
          <van-field v-model="info.oltPort" label="OLT 端口" placeholder="例如: 0/1/3" />
          <van-field
            v-model.number="info.signal"
            label="收光功率"
            placeholder="-25.0"
            type="number"
          >
            <template #button><span class="unit">dBm</span></template>
          </van-field>
        </van-cell-group>
        <div class="pane-body">
          <van-button type="primary" block round native-type="submit" :loading="infoSaving">
            下一步
          </van-button>
        </div>
      </van-form>
    </section>

    <!-- Step 3: 现场照片 -->
    <section v-else-if="currentStep === 2" class="step-pane">
      <van-cell-group inset>
        <van-cell title="必传照片" :value="`${photos.length}/3`" />
      </van-cell-group>
      <van-uploader v-model="photoFiles" :after-read="afterReadPhoto" :max-count="6" multiple>
        <template #default>
          <div class="upload-trigger">
            <van-icon name="photograph" size="28" />
            <span>点击拍照</span>
          </div>
        </template>
      </van-uploader>
      <div class="pane-body">
        <van-button type="primary" block round :disabled="photos.length < 3" @click="currentStep = 3">
          下一步
        </van-button>
      </div>
    </section>

    <!-- Step 4: 客户签名 -->
    <section v-else-if="currentStep === 3" class="step-pane">
      <van-cell-group inset>
        <van-field v-model="customerName" label="客户姓名" placeholder="请输入客户姓名" />
      </van-cell-group>
      <SignaturePad ref="sigRef" class="sig-wrap" />
      <div class="pane-body">
        <van-button type="primary" block round :loading="signing" @click="onSignSave">
          保存签名并下一步
        </van-button>
      </div>
    </section>

    <!-- Step 5: 完工提交 -->
    <section v-else class="step-pane">
      <van-cell-group inset title="完工摘要">
        <van-cell title="ONU MAC" :value="info.onuMac" />
        <van-cell title="收光功率" :value="`${info.signal} dBm`" />
        <van-cell title="OLT 端口" :value="info.oltPort" />
        <van-cell title="照片数量" :value="`${photos.length} 张`" />
        <van-cell title="签名状态">
          <template #value>
            <van-tag :type="signature ? 'success' : 'danger'">
              {{ signature ? '已采集' : '未采集' }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
      <div class="pane-body">
        <van-field v-model="remark" type="textarea" rows="3" placeholder="备注(可选)" />
        <van-button
          type="primary"
          block
          round
          :loading="submitting"
          :disabled="!canSubmit"
          @click="onSubmit"
        >
          提交完工
        </van-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showFailToast } from 'vant'
import type { UploaderFileListItem } from 'vant'
import { useWorkOrderStore } from '@/stores/workorder'
import { acceptWorkOrder, startWorkOrder } from '@/api/workorder'
import { arriveAtSite, saveInstallInfo, uploadPhoto, saveSignature, submitComplete } from '@/api/install'
import { getCurrentPosition } from '@/utils/geo'
import SignaturePad from '@/components/SignaturePad.vue'
import type { WorkOrder } from '@/types/workorder'
import type { InstallInfo, PhotoMeta, SignatureMeta } from '@/types/install'

const props = defineProps<{ id: string }>()
const router = useRouter()
const store = useWorkOrderStore()

const stepDefs = [
  { key: 'arrive', title: '到达' },
  { key: 'info', title: '信息' },
  { key: 'photos', title: '照片' },
  { key: 'sign', title: '签名' },
  { key: 'submit', title: '提交' }
]

const wo = ref<WorkOrder | null>(null)
const currentStep = ref(0)
const sigRef = ref<InstanceType<typeof SignaturePad> | null>(null)

const myLoc = ref<{ lng: number; lat: number; accuracy: number } | null>(null)
const arrivedAt = ref<number | null>(null)
const arriving = ref(false)

const info = reactive<InstallInfo>({ onuMac: '', onuSn: '', oltPort: '', signal: -25 })
const infoSaving = ref(false)

const photos = ref<PhotoMeta[]>([])
const photoFiles = ref<UploaderFileListItem[]>([])

const customerName = ref('')
const signature = ref<SignatureMeta | null>(null)
const signing = ref(false)
const remark = ref('')
const submitting = ref(false)

const canSubmit = computed(() => !!arrivedAt.value && !!info.onuMac && !!signature.value && photos.value.length >= 3)

async function loadWO() {
  wo.value = await store.fetchDetail(props.id)
}

async function refreshGps() {
  try {
    myLoc.value = await getCurrentPosition()
  } catch {
    showToast('请开启定位权限')
  }
}

/**
 * The state machine requires DISPATCHED → ACCEPTED → IN_PROGRESS before the
 * install steps run. Accept (and start) the order here so deep links into the
 * install page cannot leave the order stuck in DISPATCHED.
 */
async function ensureStarted(): Promise<boolean> {
  if (!wo.value) return false
  let cur = wo.value.status
  if (cur === 'DISPATCHED') {
    const accepted = await acceptWorkOrder(props.id)
    wo.value = accepted
    cur = accepted.status
  }
  if (cur === 'ACCEPTED') {
    const started = await startWorkOrder(props.id)
    wo.value = started
    cur = started.status
  }
  if (cur !== 'IN_PROGRESS') {
    showFailToast(`当前状态 (${wo.value?.statusDesc || cur}) 无法开工`)
    return false
  }
  return true
}

async function confirmArrive() {
  if (!myLoc.value || !wo.value) return
  arriving.value = true
  try {
    if (!(await ensureStarted())) return
    await arriveAtSite(props.id, { lng: myLoc.value.lng, lat: myLoc.value.lat, address: wo.value.installAddress })
    arrivedAt.value = Date.now()
    showSuccessToast('已记录到达')
    currentStep.value = 1
  } catch (e: unknown) {
    showFailToast(e instanceof Error ? e.message : '到达失败')
  } finally {
    arriving.value = false
  }
}

async function onInfoNext() {
  infoSaving.value = true
  try {
    await saveInstallInfo(props.id, { ...info })
    currentStep.value = 2
  } catch {
    showToast('保存失败')
  } finally {
    infoSaving.value = false
  }
}

/**
 * Upload each photo to the backend immediately — the complete endpoint
 * validates the photo count from the persisted install record, so local-only
 * metadata would fail the "至少需要 3 张现场照片" check.
 */
async function afterReadPhoto(file: UploaderFileListItem | UploaderFileListItem[]) {
  const items = Array.isArray(file) ? file : [file]
  for (const item of items) {
    const meta: PhotoMeta = {
      objectKey: `h5_${Date.now()}.jpg`,
      url: typeof item.content === 'string' ? item.content : '',
      name: item.file?.name || `photo-${Date.now()}.jpg`,
      size: item.file?.size || 0,
      capturedAt: Date.now()
    }
    try {
      await uploadPhoto(props.id, meta)
      photos.value.push(meta)
    } catch (e: unknown) {
      const idx = photoFiles.value.indexOf(item)
      if (idx >= 0) photoFiles.value.splice(idx, 1)
      showFailToast(e instanceof Error ? e.message : '照片上传失败')
    }
  }
}

async function onSignSave() {
  if (!customerName.value) { showToast('请输入客户姓名'); return }
  if (!sigRef.value || sigRef.value.isEmpty()) { showToast('请先签名'); return }
  signing.value = true
  try {
    const dataUrl = sigRef.value.toDataURL()
    const sigMeta: SignatureMeta = {
      customerName: customerName.value,
      dataUrl,
      objectKey: '',
      capturedAt: Date.now()
    }
    const res = await saveSignature(props.id, sigMeta)
    sigMeta.objectKey = res.objectKey
    signature.value = sigMeta
    currentStep.value = 4
  } catch {
    showToast('签名保存失败')
  } finally {
    signing.value = false
  }
}

async function onSubmit() {
  if (!canSubmit.value) { showToast('请检查所有步骤是否完成'); return }
  if (!wo.value) return
  submitting.value = true
  try {
    await submitComplete(props.id, {
      workOrderId: Number(props.id),
      orderId: wo.value.orderId,
      lng: myLoc.value!.lng,
      lat: myLoc.value!.lat,
      distance: 0,
      info,
      photos: photos.value,
      signature: signature.value!,
      remark: remark.value
    })
    showSuccessToast('已完工')
    setTimeout(() => router.replace('/workorders'), 800)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '提交失败'
    showToast(msg)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    await loadWO()
  } catch (e: unknown) {
    showFailToast(e instanceof Error ? e.message : '工单加载失败')
  }
  await refreshGps()
})
</script>

<style lang="scss" scoped>
.install-page {
  min-height: 100vh;
  background: var(--bbpms-bg);
  padding-bottom: 24px;
}
.steps {
  background: #fff;
  padding: 16px 8px;
  position: sticky;
  top: 46px;
  z-index: 9;
}
.step-pane {
  padding: 8px 0 16px;
}
.pane-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.unit { color: #969799; font-size: 13px; }
.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: #f7f8fa;
  border-radius: 8px;
  color: #969799;
  font-size: 12px;
  gap: 4px;
  margin: 12px 16px;
}
.sig-wrap { margin: 12px 16px; }
</style>
