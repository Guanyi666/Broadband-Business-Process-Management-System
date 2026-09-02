<template>
  <div class="nf">
    <van-nav-bar title="页面不存在" left-arrow @click-left="$router.back()" />
    <van-empty description="您访问的页面不存在">
      <van-button round type="primary" @click="$router.replace('/workorders')">返回首页</van-button>
    </van-empty>
  </div>
</template>

<style lang="scss" scoped>
.nf { min-height: 100vh; background: var(--bbpms-bg); padding-top: 46px; }
</style>
