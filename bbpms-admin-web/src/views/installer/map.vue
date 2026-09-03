<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { listInstallerLocations } from '@/api/installer'
import type { InstallerLocation } from '@/types/installer'
import PageHeader from '@/components/PageHeader.vue'

const mapEl = ref<HTMLDivElement>()
const locations = ref<InstallerLocation[]>([])
const loading = ref(false)
const loadError = ref('')
const mapError = ref('')
let timer: ReturnType<typeof setInterval> | null = null
let map: any = null
let markers: any[] = []

async function refresh() {
  loading.value = true
  loadError.value = ''
  try {
    locations.value = await listInstallerLocations()
    updateMarkers()
  } catch (error: any) {
    locations.value = []
    loadError.value = error?.message || 'Installer locations could not be loaded.'
  } finally {
    loading.value = false
  }
}

async function initMap() {
  const key = import.meta.env.VITE_AMAP_KEY
  if (!key || key.startsWith('__') || !mapEl.value) {
    mapError.value = 'Map key is not configured. Installer location data is shown in the list below.'
    return
  }
  await new Promise<void>((resolve, reject) => {
    if ((window as any).AMap) return resolve()
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Map service failed to load'))
    document.head.appendChild(script)
  })
  const AMap = (window as any).AMap
  if (!AMap) return
  map = new AMap.Map(mapEl.value, { zoom: 11, center: [116.397, 39.908] })
  updateMarkers()
}

function updateMarkers() {
  if (!map || !(window as any).AMap) return
  const AMap = (window as any).AMap
  if (markers.length) {
    markers.forEach((m) => map.remove(m))
    markers = []
  }
  locations.value.filter((l) => Number.isFinite(l.lng) && Number.isFinite(l.lat) && (l.lng !== 0 || l.lat !== 0)).forEach((l) => {
    const marker = new AMap.Marker({
      position: [l.lng, l.lat],
      title: l.name,
      icon: new AMap.Icon({
        size: new AMap.Size(28, 28),
        image: l.online
          ? 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
          : 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png',
        imageSize: new AMap.Size(28, 28)
      })
    })
    marker.on('click', () => {
      const info = new AMap.InfoWindow({
        content: `
          <div style="padding:8px 12px">
            <div style="font-weight:600">${l.name}</div>
            <div>Phone: ${l.phone || '-'}</div>
            <div>Status: ${l.online ? 'Online' : 'Offline'}</div>
            <div>Workload: ${l.workload ?? 0}</div>
            <div>Rating: ${l.rating ?? '-'}</div>
          </div>
        `
      })
      info.open(map, marker.getPosition())
    })
    markers.push(marker)
    map.add(marker)
  })
}

onMounted(async () => {
  await refresh()
  try {
    await initMap()
  } catch (error: any) {
    mapError.value = error?.message || 'Map service failed to load.'
  }
  timer = setInterval(refresh, 15000)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  map?.destroy()
})
</script>

<template>
  <div class="app-container" v-loading="loading">
    <PageHeader title="装维地图">
      <template #extra>
        <el-button @click="refresh"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </template>
    </PageHeader>
    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" class="map-alert" />
    <el-alert v-if="mapError" :title="mapError" type="warning" show-icon :closable="false" class="map-alert" />
    <div v-show="!mapError" class="map-wrapper">
      <div v-if="locations.length === 0" class="map-empty">
        <el-empty description="暂无装维位置信息" :image-size="80" />
      </div>
      <div ref="mapEl" class="map" />
    </div>
    <div class="app-card location-list">
      <el-table :data="locations" stripe empty-text="No installer locations">
        <el-table-column prop="name" label="装维人员" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.online ? 'success' : 'info'">{{ row.online ? 'Online' : 'Offline' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="workload" label="当前负载" width="100" />
        <el-table-column label="Coordinates" min-width="180">
          <template #default="{ row }">{{ row.lng && row.lat ? `${row.lng.toFixed(6)}, ${row.lat.toFixed(6)}` : '-' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.map-wrapper {
  position: relative;
  background: #fff;
  border-radius: $radius-base;
  overflow: hidden;
  height: 70vh;
  .map {
    width: 100%;
    height: 100%;
  }
  .map-empty {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;
  }
}
.map-alert { margin-bottom: 12px; }
.location-list { margin-top: 16px; }
</style>
