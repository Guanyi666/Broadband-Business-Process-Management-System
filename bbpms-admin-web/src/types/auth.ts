export interface LoginPayload {
  username: string
  password: string
  captchaId?: string
  captcha?: string
}

export interface LoginResult {
  token: string
  refreshToken?: string
  expiresIn?: number
}

export interface CaptchaResult {
  captchaId: string
  image: string
}

export interface UserInfo {
  id: number | string
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  deptId?: number | string
  deptName?: string
  roles: string[]
  permissions: string[]
}

export interface MenuNode {
  id: number | string
  parentId: number | string | null
  name: string
  path?: string
  component?: string
  icon?: string
  type: 1 | 2 | 3 // dir/menu/btn
  perm?: string
  sort?: number
  visible?: boolean
  status?: number
  children?: MenuNode[]
}

export interface RoleInfo {
  id: number | string
  code: string
  name: string
  remark?: string
  status?: number
  menus?: (number | string)[]
}

export interface DeptNode {
  id: number | string
  parentId: number | string | null
  name: string
  leader?: string
  phone?: string
  sort?: number
  status?: number
  children?: DeptNode[]
}