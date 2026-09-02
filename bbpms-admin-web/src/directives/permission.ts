import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

function check(perm: string | string[]): boolean {
  const auth = useAuthStore()
  return auth.hasPermission(perm)
}

function update(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
  const value = binding.value
  if (!value) return
  if (!check(value)) {
    el.parentNode?.removeChild(el)
  }
}

export const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    update(el, binding)
  },
  updated(el, binding) {
    update(el, binding)
  }
}