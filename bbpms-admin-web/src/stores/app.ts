import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TagItem {
  path: string
  fullPath: string
  name?: string
  title: string
  affix?: boolean
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const device = ref<'desktop' | 'mobile'>('desktop')
  const visitedViews = ref<TagItem[]>([])

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addVisitedView(view: TagItem) {
    if (visitedViews.value.some((v) => v.path === view.path)) return
    visitedViews.value.push(view)
  }

  function removeVisitedView(path: string) {
    const idx = visitedViews.value.findIndex((v) => v.path === path)
    if (idx > -1 && !visitedViews.value[idx].affix) {
      visitedViews.value.splice(idx, 1)
    }
  }

  function removeOtherViews(path: string) {
    visitedViews.value = visitedViews.value.filter((v) => v.affix || v.path === path)
  }

  function removeAllViews() {
    visitedViews.value = visitedViews.value.filter((v) => v.affix)
  }

  return {
    sidebarCollapsed,
    device,
    visitedViews,
    toggleSidebar,
    addVisitedView,
    removeVisitedView,
    removeOtherViews,
    removeAllViews
  }
})