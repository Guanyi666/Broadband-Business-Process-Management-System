export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  [k: string]: any
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface ApiOk<T> {
  code: number
  message: string
  data: T
  success?: boolean
}