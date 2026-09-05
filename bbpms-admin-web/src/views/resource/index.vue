<script setup lang="ts">
/**
 * 网络资源台账（ITERATION 2）
 * 面板1：小区 → 楼栋 → 单元 → 房间 四级联动
 * 面板2：OLT → PON → ONU 设备台账
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import PermissionButton from '@/components/PermissionButton.vue'
import {
  listRegions, createRegion,
  listCommunities, createCommunity,
  listBuildings, createBuilding,
  listUnits, createUnit,
  listRooms, createRoom,
  listOlts, createOlt,
  listPons, createPon,
  listOnus, createOnu,
  type RegionItem, type CommunityItem, type BuildingItem,
  type UnitItem, type RoomItem, type OltItem, type PonItem, type OnuItem
} from '@/api/resource'

// ---------------- 面板1：小区/楼栋/单元/房间 ----------------
const regions = ref<RegionItem[]>([])
const communities = ref<CommunityItem[]>([])
const buildings = ref<BuildingItem[]>([])
const units = ref<UnitItem[]>([])
const rooms = ref<RoomItem[]>([])

const selRegion = ref<number | undefined>(undefined)
const selCommunity = ref<number | undefined>(undefined)
const selBuilding = ref<number | undefined>(undefined)
const selUnit = ref<number | undefined>(undefined)

async function loadRegions() {
  regions.value = await listRegions()
}
async function loadCommunities() {
  communities.value = selRegion.value ? await listCommunities(selRegion.value) : []
  selCommunity.value = undefined
  buildings.value = []
  units.value = []
  rooms.value = []
}
async function loadBuildings() {
  buildings.value = selCommunity.value ? await listBuildings(selCommunity.value) : []
  selBuilding.value = undefined
  units.value = []
  rooms.value = []
}
async function loadUnits() {
  units.value = selBuilding.value ? await listUnits(selBuilding.value) : []
  selUnit.value = undefined
  rooms.value = []
}
async function loadRooms() {
  rooms.value = selUnit.value ? await listRooms(selUnit.value) : []
}

// 新增表单（按层级动态显示需要的字段）
const addForm = reactive({
  regionName: '', regionCode: '',
  communityName: '', communityAddress: '',
  buildingName: '', totalFloors: undefined as number | undefined,
  unitName: '',
  roomNo: ''
})
const addDialog = ref(false)
const addKind = ref<'REGION' | 'COMMUNITY' | 'BUILDING' | 'UNIT' | 'ROOM'>('REGION')

function openAdd(kind: 'REGION' | 'COMMUNITY' | 'BUILDING' | 'UNIT' | 'ROOM') {
  addKind.value = kind
  Object.assign(addForm, {
    regionName: '', regionCode: '', communityName: '', communityAddress: '',
    buildingName: '', totalFloors: undefined, unitName: '', roomNo: ''
  })
  addDialog.value = true
}

async function submitAdd() {
  try {
    switch (addKind.value) {
      case 'REGION':
        if (!addForm.regionName.trim() || !addForm.regionCode.trim()) { ElMessage.warning('请输入区域名称和编码'); return }
        await createRegion(addForm.regionName.trim(), addForm.regionCode.trim())
        await loadRegions()
        break
      case 'COMMUNITY':
        if (selRegion.value == null) { ElMessage.warning('请先选择区域'); return }
        if (!addForm.communityName.trim()) { ElMessage.warning('请输入小区名称'); return }
        await createCommunity(selRegion.value, addForm.communityName.trim(), addForm.communityAddress.trim())
        await loadCommunities()
        break
      case 'BUILDING':
        if (selCommunity.value == null) { ElMessage.warning('请先选择小区'); return }
        if (!addForm.buildingName.trim()) { ElMessage.warning('请输入楼栋名称'); return }
        if (!addForm.totalFloors || addForm.totalFloors < 1 || addForm.totalFloors > 200) { ElMessage.warning('请输入 1-200 的楼层数'); return }
        await createBuilding(selCommunity.value, addForm.buildingName.trim(), addForm.totalFloors)
        await loadBuildings()
        break
      case 'UNIT':
        if (selBuilding.value == null) { ElMessage.warning('请先选择楼栋'); return }
        if (!addForm.unitName.trim()) { ElMessage.warning('请输入单元名称'); return }
        await createUnit(selBuilding.value, addForm.unitName.trim())
        await loadUnits()
        break
      case 'ROOM':
        if (selUnit.value == null) { ElMessage.warning('请先选择单元'); return }
        if (!addForm.roomNo.trim()) { ElMessage.warning('请输入房号'); return }
        await createRoom(selUnit.value, addForm.roomNo.trim())
        await loadRooms()
        break
    }
    addDialog.value = false
    ElMessage.success('已创建')
  } catch (e: any) {
    ElMessage.error(e?.msg || e?.message || '创建失败')
  }
}

// ---------------- 面板2：OLT / PON / ONU ----------------
const olts = ref<OltItem[]>([])
const pons = ref<PonItem[]>([])
const onus = ref<OnuItem[]>([])
const selOlt = ref<number | undefined>(undefined)
const selPon = ref<number | undefined>(undefined)

const oltRegion = ref<number | undefined>(undefined)

async function loadOlts() {
  olts.value = await listOlts(oltRegion.value)
  selOlt.value = undefined
  pons.value = []
  onus.value = []
}
async function loadPons() {
  pons.value = selOlt.value ? await listPons(selOlt.value) : []
  selPon.value = undefined
  onus.value = []
}
async function loadOnus() {
  onus.value = selPon.value ? await listOnus(undefined, undefined).then((list) => list.filter((o) => o.ponId === selPon.value)) : []
}

// OLT/PON/ONU 新增
const devForm = reactive({ oltName: '', oltIp: '', oltVendor: '', oltModel: '', ponName: '', totalPorts: 32, onuSn: '', onuModel: '' })
const devDialog = ref(false)
const devKind = ref<'OLT' | 'PON' | 'ONU'>('OLT')

function openDevAdd(kind: 'OLT' | 'PON' | 'ONU') {
  devKind.value = kind
  Object.assign(devForm, { oltName: '', oltIp: '', oltVendor: '', oltModel: '', ponName: '', totalPorts: 32, onuSn: '', onuModel: '' })
  devDialog.value = true
}

async function submitDevAdd() {
  try {
    switch (devKind.value) {
      case 'OLT':
        await createOlt(devForm.oltName, oltRegion.value || 1, devForm.oltIp, devForm.oltVendor, devForm.oltModel)
        await loadOlts()
        break
      case 'PON':
        if (selOlt.value == null) { ElMessage.warning('请先选择 OLT'); return }
        await createPon(selOlt.value, devForm.ponName, devForm.totalPorts)
        await loadPons()
        break
      case 'ONU':
        if (selPon.value == null) { ElMessage.warning('请先选择 PON'); return }
        await createOnu(devForm.onuSn, devForm.onuModel)
        await loadOnus()
        break
    }
    devDialog.value = false
    ElMessage.success('已创建')
  } catch (e: any) {
    ElMessage.error(e?.msg || e?.message || '创建失败')
  }
}

onMounted(async () => {
  await loadRegions()
  await loadOlts()
})

const regionName = computed(() => regions.value.find((r) => r.id === selRegion.value)?.name || '')
const oltName = computed(() => olts.value.find((o) => o.id === selOlt.value)?.name || '')
const communityName = computed(() => communities.value.find((c) => c.id === selCommunity.value)?.name || '')
const buildingName = computed(() => buildings.value.find((b) => b.id === selBuilding.value)?.name || '')
</script>

<template>
  <div class="app-container">
    <PageHeader title="网络资源台账" description="区域 / 小区 / 楼栋 / 单元 / 房间 + OLT / PON / ONU 末端资源" />

    <el-row :gutter="16">
      <!-- 面板1：地址资源 -->
      <el-col :span="14">
        <div class="app-card">
          <div class="card-head">
            <span class="card-title">地址资源</span>
            <PermissionButton :permission="'resource:edit'">
              <el-button
                v-if="selRegion && !selCommunity"
                type="primary" size="small" @click="openAdd('COMMUNITY')">新增小区</el-button>
              <el-button
                v-else-if="selRegion && selCommunity && !selBuilding"
                type="primary" size="small" @click="openAdd('BUILDING')">新增楼栋</el-button>
              <el-button
                v-else-if="selRegion && selCommunity && selBuilding && !selUnit"
                type="primary" size="small" @click="openAdd('UNIT')">新增单元</el-button>
              <el-button
                v-else-if="selRegion && selCommunity && selBuilding && selUnit"
                type="primary" size="small" @click="openAdd('ROOM')">新增房间</el-button>
            </PermissionButton>
          </div>

          <el-form label-width="70px" class="mt-8">
            <el-form-item label="区域">
              <el-select v-model="selRegion" placeholder="选择区域" style="width: 220px" @change="loadCommunities">
                <el-option v-for="r in regions" :key="r.id" :label="`${r.name} (${r.code})`" :value="r.id" />
              </el-select>
              <PermissionButton :permission="'resource:edit'">
                <el-button size="small" class="ml-8" @click="openAdd('REGION')">新增区域</el-button>
              </PermissionButton>
            </el-form-item>

            <el-form-item v-if="selRegion" label="小区">
              <el-select v-model="selCommunity" placeholder="选择小区" style="width: 320px" @change="loadBuildings">
                <el-option v-for="c in communities" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="selCommunity" label="楼栋">
              <el-select v-model="selBuilding" placeholder="选择楼栋" style="width: 320px" @change="loadUnits">
                <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="selBuilding" label="单元">
              <el-select v-model="selUnit" placeholder="选择单元" style="width: 320px" @change="loadRooms">
                <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-form>

          <el-table v-if="selUnit" :data="rooms" size="small" stripe max-height="360" class="mt-8">
            <el-table-column prop="roomNo" label="房号" width="120" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag v-if="row.isInstalled === 1" type="success">已安装</el-tag>
                <el-tag v-else type="info">未安装</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 面板2：设备台账 -->
      <el-col :span="10">
        <div class="app-card">
          <div class="card-head">
            <span class="card-title">设备台账</span>
            <PermissionButton :permission="'resource:edit'">
              <el-button
                v-if="!selOlt" type="primary" size="small" @click="openDevAdd('OLT')">新增 OLT</el-button>
              <el-button
                v-else-if="selOlt && !selPon" type="primary" size="small" @click="openDevAdd('PON')">新增 PON</el-button>
              <el-button
                v-else-if="selOlt && selPon" type="primary" size="small" @click="openDevAdd('ONU')">新增 ONU</el-button>
            </PermissionButton>
          </div>

          <el-form label-width="70px" class="mt-8">
            <el-form-item label="OLT">
              <el-select v-model="selOlt" placeholder="选择 OLT" style="width: 100%" @change="loadPons">
                <el-option v-for="o in olts" :key="o.id" :label="`${o.name} (${o.model || '—'})`" :value="o.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selOlt" label="PON">
              <el-select v-model="selPon" placeholder="选择 PON 口" style="width: 100%" @change="loadOnus">
                <el-option v-for="p in pons" :key="p.id" :label="`${p.name} (${p.usedPorts}/${p.totalPorts})`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-form>

          <el-table v-if="selPon" :data="onus" size="small" stripe max-height="300" class="mt-8">
            <el-table-column prop="sn" label="SN" width="160" />
            <el-table-column prop="model" label="型号" width="110" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 'IN_STOCK' ? 'info' : 'success'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 新增地址资源对话框 -->
    <el-dialog v-model="addDialog" :title="`新增${({ REGION: '区域', COMMUNITY: '小区', BUILDING: '楼栋', UNIT: '单元', ROOM: '房间' } as any)[addKind]}`" width="440px">
      <el-form label-width="80px">
        <template v-if="addKind === 'REGION'">
          <el-form-item label="区域名称"><el-input v-model="addForm.regionName" placeholder="如：北京市" /></el-form-item>
          <el-form-item label="区域编码"><el-input v-model="addForm.regionCode" placeholder="如：BJ" /></el-form-item>
        </template>
        <template v-else-if="addKind === 'COMMUNITY'">
          <el-form-item label="所属区域"><el-input :model-value="regionName" disabled /></el-form-item>
          <el-form-item label="小区名称"><el-input v-model="addForm.communityName" placeholder="如：朝阳区演示小区" /></el-form-item>
          <el-form-item label="地址"><el-input v-model="addForm.communityAddress" /></el-form-item>
        </template>
        <template v-else-if="addKind === 'BUILDING'">
          <el-form-item label="所属小区"><el-input :model-value="communityName" disabled /></el-form-item>
          <el-form-item label="楼栋名称"><el-input v-model="addForm.buildingName" placeholder="如：3号楼" /></el-form-item>
          <el-form-item label="总层数"><el-input-number v-model="addForm.totalFloors" :min="1" /></el-form-item>
        </template>
        <template v-else-if="addKind === 'UNIT'">
          <el-form-item label="所属楼栋"><el-input :model-value="buildingName" disabled /></el-form-item>
          <el-form-item label="单元名称"><el-input v-model="addForm.unitName" placeholder="如：1单元" /></el-form-item>
        </template>
        <template v-else-if="addKind === 'ROOM'">
          <el-form-item label="单元"><el-input :model-value="units.find((u) => u.id === selUnit)?.name || ''" disabled /></el-form-item>
          <el-form-item label="房号"><el-input v-model="addForm.roomNo" placeholder="如：101" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增设备对话框 -->
    <el-dialog v-model="devDialog" :title="`新增${({ OLT: 'OLT', PON: 'PON', ONU: 'ONU' } as any)[devKind]}`" width="440px">
      <el-form label-width="80px">
        <template v-if="devKind === 'OLT'">
          <el-form-item label="OLT 名称"><el-input v-model="devForm.oltName" placeholder="如：OLT-BJ-CY-02" /></el-form-item>
          <el-form-item label="IP"><el-input v-model="devForm.oltIp" placeholder="如：10.1.1.2" /></el-form-item>
          <el-form-item label="厂商"><el-input v-model="devForm.oltVendor" placeholder="如：Huawei" /></el-form-item>
          <el-form-item label="型号"><el-input v-model="devForm.oltModel" placeholder="如：MA5800" /></el-form-item>
        </template>
        <template v-else-if="devKind === 'PON'">
          <el-form-item label="所属 OLT"><el-input :model-value="oltName" disabled /></el-form-item>
          <el-form-item label="PON 口名"><el-input v-model="devForm.ponName" placeholder="如：1/1/3" /></el-form-item>
          <el-form-item label="端口数"><el-input-number v-model="devForm.totalPorts" :min="1" /></el-form-item>
        </template>
        <template v-else-if="devKind === 'ONU'">
          <el-form-item label="所属 PON"><el-input :model-value="pons.find((p) => p.id === selPon)?.name || ''" disabled /></el-form-item>
          <el-form-item label="SN"><el-input v-model="devForm.onuSn" placeholder="如：HBH-ONY-0003" /></el-form-item>
          <el-form-item label="型号"><el-input v-model="devForm.onuModel" placeholder="如：HG8245H" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="devDialog = false">取消</el-button>
        <el-button type="primary" @click="submitDevAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-weight: 600;
  font-size: 15px;
}
.mt-8 {
  margin-top: 8px;
}
.ml-8 {
  margin-left: 8px;
}
</style>