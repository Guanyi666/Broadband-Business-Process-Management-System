<script setup lang="ts">
/**
 * 客户 H5「履约进度」可视化组件。
 * 垂直 Step 展示 5 节点骨架（已创建 → 已审核 → 已派单 → 上门安装 → 已完成），
 * 时间严格取后端真实字段；支持 SKIP 态与异常态；附催单按钮（防刷倒计时 + 呼吸灯）。
 */
import { computed, ref } from 'vue'
import type { CustomerTrack, CustomerTrackStage } from '@/types'

const props = defineProps<{ track: CustomerTrack | null }>()

const emit = defineEmits<{ (e: 'urged'): void }>()

const urging = ref(false)

/** 当前节点索引（0 基） */
const activeIndex = computed(() => {
  const s = props.track?.stages || []
  const current = s.findIndex((x) => x.state === 'CURRENT')
  if (current >= 0) return current
  const pending = s.findIndex((x) => x.state === 'PENDING')
  return pending >= 0 ? pending : -1
})

const iconOf = (s: CustomerTrackStage): string => {
  switch (s.state) {
    case 'DONE': return '✓'
    case 'CURRENT': return '●'
    case 'EXCEPTION': return '!'
    case 'SKIP': return '↷'
    default: return '○'
  }
}

const stateText = (s: CustomerTrackStage): string => {
  if (s.time) return s.time
  if (s.state === 'SKIP') return s.remark || '系统自动跳过'
  if (s.state === 'EXCEPTION') return '异常'
  return '待处理'
}

/** 防刷冷却倒计时（秒） */
const coolDownLeft = ref(props.track?.urgeCoolDownSeconds || 0)
let coolDownTimer: ReturnType<typeof setInterval> | undefined

function startCoolDown(seconds: number) {
  coolDownLeft.value = seconds
  clearInterval(coolDownTimer)
  coolDownTimer = setInterval(() => {
    coolDownLeft.value -= 1
    if (coolDownLeft.value <= 0) {
      clearInterval(coolDownTimer)
      coolDownLeft.value = 0
    }
  }, 1000)
}

const canUrge = computed(() => !urging.value && !props.track?.terminal && coolDownLeft.value <= 0 && !!props.track?.canUrge)

async function onUrge() {
  if (!props.track || urging.value) return
  urging.value = true
  try {
    emit('urged')
  } finally {
    urging.value = false
  }
}
</script>

<template>
  <div v-if="track" class="progress-wrap">
    <!-- 顶部状态卡 -->
    <div class="track-head">
      <div class="track-head__status">{{ track.statusLabel }}</div>
      <div class="track-head__progress">{{ track.progress }} · 套餐 {{ track.packageName }}</div>
    </div>

    <!-- 垂直 Step 骨架 -->
    <div class="track-steps">
      <div
        v-for="(s, i) in track.stages"
        :key="s.code"
        class="track-step"
        :class="[`is-${s.state.toLowerCase()}`, { 'is-active': i === activeIndex }]"
      >
        <div class="track-step__rail">
          <div class="track-step__dot">{{ iconOf(s) }}</div>
          <div v-if="i < track.stages.length - 1" class="track-step__line" />
        </div>
        <div class="track-step__body">
          <div class="track-step__name">
            {{ s.name }}
            <span v-if="s.state === 'SKIP'" class="badge badge-skip">已跳过</span>
            <span v-else-if="s.state === 'EXCEPTION'" class="badge badge-exception">异常</span>
          </div>
          <div class="track-step__time">{{ stateText(s) }}</div>
        </div>
      </div>
    </div>

    <!-- 催单按钮 -->
    <div v-if="!track.terminal && (track.canUrge || coolDownLeft > 0)" class="urge-area">
      <button
        class="urge-btn"
        :class="{ 'is-breathe': track.canUrge && coolDownLeft <= 0, 'is-cooling': coolDownLeft > 0 }"
        :disabled="!canUrge"
        @click="onUrge"
      >
        <span v-if="coolDownLeft > 0">已催单，{{ coolDownLeft }} 秒后可再次催单</span>
        <span v-else-if="urging">催单中…</span>
        <span v-else>催单</span>
      </button>
      <div v-if="track.canUrge && coolDownLeft <= 0" class="urge-tip">
        当前环节等待较久，点击催单我们将加急处理
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.progress-wrap { padding: 4px 0; }

.track-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; margin-bottom: 12px;
  border-radius: 12px; background: linear-gradient(135deg, #1267e5 0%, #3b82f6 100%); color: #fff;
}
.track-head__status { font-size: 16px; font-weight: 700; }
.track-head__progress { font-size: 12px; opacity: .92; }

/* 垂直步骤 */
.track-steps { display: flex; flex-direction: column; }
.track-step { display: flex; gap: 12px; position: relative; }
.track-step__rail { display: flex; flex-direction: column; align-items: center; width: 26px; flex: 0 0 26px; }
.track-step__dot {
  width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff; background: #c3cad6;
  transition: all .3s ease; z-index: 1;
}
.track-step__line { width: 2px; flex: 1; background: #e2e8f0; margin: 2px 0; transition: background .3s ease; }
.track-step__body { flex: 1; padding: 2px 0 22px; }
.track-step__name { font-size: 15px; font-weight: 600; color: #333; display: flex; align-items: center; gap: 6px; }
.track-step__time { font-size: 12px; color: #939bac; margin-top: 3px; }

.track-step.is-done .track-step__dot { background: #10b981; }
.track-step.is-done .track-step__line { background: #10b981; }
.track-step.is-current .track-step__dot {
  background: #1267e5; box-shadow: 0 0 0 4px rgba(18, 103, 229, .15);
}
.track-step.is-current .track-step__name { color: #1267e5; }
.track-step.is-current .track-step__line { background: linear-gradient(180deg, #1267e5, #e2e8f0); }
.track-step.is-pending { opacity: .55; }
.track-step.is-skip .track-step__dot { background: #a5b4cb; color: #fff; }
.track-step.is-skip .track-step__name { color: #7d8798; text-decoration: line-through; }
.track-step.is-exception .track-step__dot { background: #f43f5e; }
.track-step.is-exception .track-step__name { color: #f43f5e; }

.badge { font-size: 10px; padding: 1px 6px; border-radius: 8px; font-weight: 500; }
.badge-skip { background: #eef2f7; color: #7d8798; }
.badge-exception { background: #fee2e2; color: #f43f5e; }

/* 催单 */
.urge-area { margin-top: 4px; text-align: center; }
.urge-btn {
  width: 100%; height: 46px; border: none; border-radius: 23px;
  font-size: 16px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #ff7a45, #ff4d4f);
  cursor: pointer; transition: all .3s ease; position: relative; overflow: hidden;
}
.urge-btn:active { transform: scale(.97); }
.urge-btn.is-cooling { background: #c3cad6; color: #fff; cursor: not-allowed; }
/* 呼吸灯效果 */
.urge-btn.is-breathe { animation: breathe 2s ease-in-out infinite; }
@keyframes breathe {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 77, 79, .45); }
  50% { box-shadow: 0 0 0 12px rgba(255, 77, 79, 0); }
}
.urge-tip { font-size: 12px; color: #939bac; margin-top: 8px; }
</style>
