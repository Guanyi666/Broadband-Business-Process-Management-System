<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <el-config-provider>
    <RouterView />
  </el-config-provider>
</template>

<style lang="scss">
#app {
  height: 100%;
  font-family: $font-family-base;
}
</style>