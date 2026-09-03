<script setup lang="ts">
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import enUS from 'element-plus/es/locale/lang/en'
import { useI18n } from 'vue-i18n'

// Element Plus 组件内置文案跟随 vue-i18n 当前语言（分页、日期选择器、表格空态等）
const { locale } = useI18n()
const epLocale = computed(() => (locale.value === 'en-US' ? enUS : zhCn))
</script>

<template>
  <el-config-provider :locale="epLocale">
    <RouterView />
  </el-config-provider>
</template>

<style lang="scss">
#app {
  height: 100%;
  font-family: $font-family-base;
}
</style>