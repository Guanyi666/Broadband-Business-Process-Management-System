<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

interface Props {
  modelValue?: { lng: number; lat: number; address?: string }
  defaultCenter?: [number, number]
  zoom?: number
  readonly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  defaultCenter: () => [116.397, 39.908] as [number, number],
  zoom: 12
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: { lng: number; lat: number; address?: string }): void
  (e: 'select', v: { lng: number; lat: number; address?: string }): void
}>()

const mapEl = ref<HTMLDivElement>()
let map: any = null
let marker: any = null
let AMapRef: any = null

async function loadAmap() {
  const key = import.meta.env.VITE_AMAP_KEY
  if (!key || key.startsWith('__')) {
    // mock fallback for dev: just display a placeholder
    return null
  }
  if ((window as any).AMap) return (window as any).AMap
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    script.onload = () => resolve((window as any).AMap)
    script.onerror = reject
    document.head.appendChild(script)
  })
}

async function initMap() {
  if (!mapEl.value) return
  AMapRef = await loadAmap()
  if (!AMapRef) return

  map = new AMapRef.Map(mapEl.value, {
    zoom: props.zoom,
    center: props.defaultCenter
  })

  if (props.modelValue) {
    addMarker(props.modelValue.lng, props.modelValue.lat)
  }

  if (!props.readonly) {
    map.on('click', (e: any) => {
      const lng = e.lnglat.getLng()
      const lat = e.lnglat.getLat()
      addMarker(lng, lat)
      const v = { lng, lat, address: `${lng.toFixed(6)},${lat.toFixed(6)}` }
      emit('update:modelValue', v)
      emit('select', v)
    })
  }
}

function addMarker(lng: number, lat: number) {
  if (!AMapRef) return
  if (marker) map?.remove(marker)
  marker = new AMapRef.Marker({ position: [lng, lat] })
  map?.add(marker)
  map?.setCenter([lng, lat])
}

onMounted(() => initMap())
onBeforeUnmount(() => {
  map?.destroy()
})

watch(
  () => props.modelValue,
  (v) => {
    if (v && AMapRef) addMarker(v.lng, v.lat)
  },
  { deep: true }
)
</script>

<template>
  <div ref="mapEl" class="map-picker" :style="{ height: '320px' }">
    <div v-if="!AMapRef" class="map-fallback">
      <el-icon><Location /></el-icon>
      <span>Map disabled (AMap key missing)</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.map-picker {
  position: relative;
  width: 100%;
  border-radius: $radius-base;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}
.map-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #909399;
  background: #f5f7fa;
  gap: 8px;
}
</style>