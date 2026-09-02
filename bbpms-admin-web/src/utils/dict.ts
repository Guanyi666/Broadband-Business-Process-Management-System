// simple deep clone
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') return obj
  if (Array.isArray(obj)) return obj.map(deepClone) as unknown as T
  const out: any = {}
  for (const k in obj as any) out[k] = deepClone((obj as any)[k])
  return out
}

export function debounce<T extends (...args: any[]) => any>(fn: T, wait = 300) {
  let timer: ReturnType<typeof setTimeout> | null = null
  return function (this: any, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), wait)
  }
}

export function buildTree<T extends { id: number | string; parentId?: number | string | null }>(
  list: T[],
  rootParent: null | number | string = null
): (T & { children: any[] })[] {
  const map = new Map<any, any>()
  list.forEach((it) => map.set(it.id, { ...it, children: [] }))
  const roots: any[] = []
  map.forEach((node) => {
    if (node.parentId === rootParent || node.parentId === undefined || node.parentId === null) {
      roots.push(node)
    } else {
      const parent = map.get(node.parentId)
      if (parent) parent.children.push(node)
      else roots.push(node)
    }
  })
  return roots
}