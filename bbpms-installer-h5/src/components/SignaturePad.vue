<template>
  <div class="signature-pad">
    <div class="pad-container" :style="{ height }">
      <canvas
        ref="canvasRef"
        class="pad-canvas"
        @touchstart.prevent="onStart"
        @touchmove.prevent="onMove"
        @touchend.prevent="onEnd"
        @mousedown="onStart"
        @mousemove="onMove"
        @mouseup="onEnd"
        @mouseleave="onEnd"
      />
      <div v-if="!hasDrawn" class="placeholder">请在此处签名</div>
    </div>
    <div class="toolbar">
      <van-button size="small" plain @click="clear">清除</van-button>
      <span class="hint">提示:请使用正楷签名</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = withDefaults(
  defineProps<{ height?: string; penColor?: string; lineWidth?: number }>(),
  { height: '200px', penColor: '#000', lineWidth: 2 }
)

const canvasRef = ref<HTMLCanvasElement | null>(null)
const hasDrawn = ref(false)
let ctx: CanvasRenderingContext2D | null = null
let drawing = false
let lastX = 0
let lastY = 0
let dpr = 1

function resizeCanvas() {
  const c = canvasRef.value
  if (!c) return
  const parent = c.parentElement
  if (!parent) return
  dpr = window.devicePixelRatio || 1
  const { width } = parent.getBoundingClientRect()
  c.width = width * dpr
  c.height = parseInt(props.height) * dpr
  c.style.width = `${width}px`
  c.style.height = props.height
  if (ctx) {
    ctx.scale(dpr, dpr)
    ctx.lineWidth = props.lineWidth
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.strokeStyle = props.penColor
  }
}

function pos(e: MouseEvent | TouchEvent) {
  const c = canvasRef.value!
  const rect = c.getBoundingClientRect()
  if ('touches' in e && e.touches[0]) {
    return { x: e.touches[0].clientX - rect.left, y: e.touches[0].clientY - rect.top }
  }
  return { x: (e as MouseEvent).clientX - rect.left, y: (e as MouseEvent).clientY - rect.top }
}

function onStart(e: MouseEvent | TouchEvent) {
  if (!ctx) return
  drawing = true
  const { x, y } = pos(e)
  lastX = x; lastY = y
  hasDrawn.value = true
}

function onMove(e: MouseEvent | TouchEvent) {
  if (!drawing || !ctx) return
  const { x, y } = pos(e)
  ctx.beginPath()
  ctx.moveTo(lastX, lastY)
  ctx.lineTo(x, y)
  ctx.stroke()
  lastX = x; lastY = y
}

function onEnd() { drawing = false }

function clear() {
  if (!ctx || !canvasRef.value) return
  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  hasDrawn.value = false
}

function isEmpty() { return !hasDrawn.value }
function toDataURL() { return canvasRef.value?.toDataURL('image/png') || '' }

defineExpose({ clear, isEmpty, toDataURL })

onMounted(() => {
  if (canvasRef.value) {
    ctx = canvasRef.value.getContext('2d')
    resizeCanvas()
  }
  window.addEventListener('resize', resizeCanvas)
})
onBeforeUnmount(() => window.removeEventListener('resize', resizeCanvas))
</script>

<style lang="scss" scoped>
.signature-pad {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebedf0;
}
.pad-container { position: relative; background: #fafafa; width: 100%; }
.pad-canvas { display: block; touch-action: none; }
.placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c8c9cc;
  font-size: 16px;
  pointer-events: none;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff;
  border-top: 1px solid #ebedf0;
  .hint { font-size: 12px; color: #969799; }
}
</style>
