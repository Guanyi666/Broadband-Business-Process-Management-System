<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { listInstallerLocations } from '@/api/installer'
import type { InstallerLocation } from '@/types/installer'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const mapEl = ref<HTMLDivElement>()
const locations = ref<InstallerLocation[]>([])
let timer: ReturnType<typeof setInterval> | null = null
let map: any = null
let markers: any[] = []

async function refresh() {
  try {
    locations.value = await listInstallerLocations()
    updateMarkers()
  } catch {
    locations.value = []
  }
}

async function initMap() {
  const key = import.meta.env.VITE_AMAP_KEY
  if (!key || key.startsWith('__') || !mapEl.value) return
  await new Promise<void>((resolve) => {
    if ((window as any).AMap) return resolve()
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    script.onload = () => resolve()
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
  locations.value.forEach((l) => {
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
  await initMap()
  timer = setInterval(refresh, 15000)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  map?.destroy()
})
</script>

<template>
  <div class="app-container">
    <PageHeader title="Installer Map">
      <template #extra>
        <el-button @click="refresh"><el-icon><Refresh /></el-icon> Refresh</el-button>
      </template>
    </PageHeader>
    <div class="map-wrapper">
      <div v-if="locations.length === 0" class="map-empty">
        <el-empty description="No installer locations to display" :image-size="80" />
      </div>
      <div ref="mapEl" class="map" />
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
</style>