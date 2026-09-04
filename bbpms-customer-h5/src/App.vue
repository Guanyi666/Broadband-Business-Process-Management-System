<template>
  <div class="app-shell">
    <router-view />
    <van-tabbar v-if="showTabbar" route safe-area-inset-bottom active-color="#1267e5">
      <van-tabbar-item to="/home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/services" icon="service-o">服务</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const showTabbar = computed(() => route.meta.showTabbar === true)
</script>
