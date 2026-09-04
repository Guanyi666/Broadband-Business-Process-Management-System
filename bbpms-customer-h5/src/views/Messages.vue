<template>
  <div class="page">
    <van-nav-bar title="消息中心" left-arrow @click-left="router.back()" />
    <div class="page-body">
      <div v-for="item in messages" :key="item.id" class="card message" @click="markRead(item)">
        <div class="row"><strong>{{ item.templateCode || '业务通知' }}</strong><van-badge v-if="item.status !== 'READ'" dot /></div>
        <p>{{ item.content || '您的业务状态已更新，请查看订单详情。' }}</p>
        <span class="muted">{{ format(item.createTime) }}</span>
      </div>
      <van-empty v-if="!messages.length" description="暂无消息" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { pageMessages, readMessage } from '@/api/portal'
import type { MessageItem } from '@/types'
const router = useRouter(); const messages = ref<MessageItem[]>([])
const format = (v?: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : ''
async function load() { messages.value = (await pageMessages()).records || [] }
async function markRead(item: MessageItem) { if (item.status !== 'READ') { await readMessage(item.id); item.status = 'READ' } }
onMounted(load)
</script>
<style scoped>.message p { color: #59657a; line-height: 1.6; margin: 12px 0; }</style>
