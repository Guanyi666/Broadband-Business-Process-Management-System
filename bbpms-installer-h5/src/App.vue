<template>
  <div id="app-root">
    <router-view v-slot="{ Component, route }">
      <transition :name="String(route.meta.transition || 'slide')" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="route.fullPath" />
        </keep-alive>
      </transition>
    </router-view>
    <van-tabbar v-if="showTabbar" route safe-area-inset-bottom>
      <van-tabbar-item to="/workorders" icon="orders-o">工单</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const cachedViews = ['WorkorderList', 'ProfileIndex']
const showTabbar = computed(() => route.meta?.showTabbar === true)
</script>

<style lang="scss">
#app-root {
  min-height: 100vh;
  background: var(--bbpms-bg);
}
.slide-enter-active, .slide-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.slide-enter-from { transform: translateX(100%); opacity: 0; }
.slide-leave-to { transform: translateX(-30%); opacity: 0; }
</style>
