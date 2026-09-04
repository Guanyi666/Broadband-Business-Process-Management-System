<template>
  <div class="attendance-report">
    <PageHeader title="考勤管理 - 团队报告" :breadcrumb="['考勤', '团队报告']" />

    <el-card class="block">
      <template #header>
        <div class="hdr">
          <span>当前在岗装维</span>
          <el-button size="small" @click="loadOnDuty">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="onDuty" stripe size="small">
        <el-table-column prop="installerId" label="ID" width="80" />
        <el-table-column prop="installerName" label="姓名" />
        <el-table-column label="签到时间" width="180">
          <template #default="{ row }">
            <BBPMSStatusTag :status="row.status" :type-map="STATUS_MAP" />
            <span style="margin-left: 8px">{{ formatTime(row.clockInAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已工作">
          <template #default="{ row }">{{ formatMinutes(row.workMinutes || 0) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="block">
      <template #header>
        <div class="hdr">
          <span>装维月度统计</span>
          <el-date-picker
            v-model="month"
            type="month"
            placeholder="选择月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            style="width: 160px"
          />
        </div>
      </template>
      <el-table :data="monthlyRows" stripe size="small">
        <el-table-column prop="installerId" label="ID" width="80" />
        <el-table-column label="姓名">
          <template #default="{ row }">{{ row.installerName || ('#' + row.installerId) }}</template>
        </el-table-column>
        <el-table-column prop="yearMonth" label="月份" width="100" />
        <el-table-column label="工作时长">
          <template #default="{ row }">{{ formatMinutes(row.totalWorkMinutes) }}</template>
        </el-table-column>
        <el-table-column prop="workDays" label="工作天数" width="100" />
        <el-table-column prop="lateCount" label="迟到次数" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" link @click="onPick(row)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="picked" class="block">
      <template #header>
        <div class="hdr">
          <span>{{ picked.installerId }} - 最近 30 天</span>
          <el-button size="small" @click="picked = null">关闭</el-button>
        </div>
      </template>
      <el-table :data="pickedDetail" stripe size="small">
        <el-table-column prop="workDate" label="日期" width="120" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <BBPMSStatusTag :status="row.status" :type-map="STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="签到">
          <template #default="{ row }">{{ formatTime(row.clockInAt) }}</template>
        </el-table-column>
        <el-table-column label="签出">
          <template #default="{ row }">{{ formatTime(row.clockOutAt) }}</template>
        </el-table-column>
        <el-table-column label="工时">
          <template #default="{ row }">{{ formatMinutes(row.workMinutes || 0) }}</template>
        </el-table-column>
        <el-table-column label="迟到">
          <template #default="{ row }">
            <el-tag v-if="row.isLate" type="danger" size="small">迟</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onActivated } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchOnDuty, fetchInstallerDaily, fetchInstallerMonthly, type AttendanceVO } from '@/api/attendance'
import { pageInstallers } from '@/api/installer'
import BBPMSStatusTag from '@/components/BBPMSStatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

defineOptions({ name: 'AttendanceReport' })

const STATUS_MAP: Record<string, any> = {
  ON_DUTY: 'success',
  ON_BREAK: 'warning',
  OFF_DUTY: 'info',
  AUTO_OFF: 'danger'
}

const onDuty = ref<AttendanceVO[]>([])
const month = ref<string>(new Date().toISOString().substring(0, 7))
const monthlyRows = ref<any[]>([])
const picked = ref<any>(null)
const pickedDetail = ref<AttendanceVO[]>([])

async function loadOnDuty() {
  try { onDuty.value = await fetchOnDuty() } catch { /* noop */ }
}

async function loadMonthly() {
  if (!month.value) return
  try {
    const installers: any = await pageInstallers({ pageNum: 1, pageSize: 200 })
    const rows = await Promise.all((installers.list || []).map(async (inst: any) => {
      const m = await fetchInstallerMonthly(inst.userId || inst.id, month.value)
      return m ? { ...m, installerName: inst.name || inst.username } : null
    }))
    monthlyRows.value = rows.filter(Boolean)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  }
}

async function onPick(row: any) {
  picked.value = row
  try {
    pickedDetail.value = await fetchInstallerDaily(row.installerId, 30)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  }
}

function formatTime(iso?: string) {
  if (!iso) return '--'
  return iso.substring(11, 16)
}

function formatMinutes(min: number) {
  const h = Math.floor(min / 60); const m = min % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

watch(month, () => loadMonthly())
onMounted(() => { loadOnDuty(); loadMonthly() })
onActivated(() => { loadOnDuty(); loadMonthly() })
</script>

<style lang="scss" scoped>
.attendance-report { padding: 16px; }
.block { margin-bottom: 16px; }
.hdr { display: flex; justify-content: space-between; align-items: center; }
</style>
