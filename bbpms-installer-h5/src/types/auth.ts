export interface LoginParams {
  username: string
  password: string
  captcha?: string
  captchaId?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken?: string
  expiresIn?: number
  user: UserInfo
}

export interface CaptchaResult {
  id: string
  image: string
}

export interface UserInfo {
  id: string | number
  username: string
  name: string
  avatar?: string
  phone?: string
  deptName?: string
  teamName?: string
  roles?: string[]
  permissions?: string[]
}
