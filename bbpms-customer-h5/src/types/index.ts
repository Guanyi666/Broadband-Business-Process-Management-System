export interface UserInfo {
  id: number | string
  username: string
  name: string
  roles?: string[]
  permissions?: string[]
}

export interface LoginResult {
  accessToken: string
  token?: string
  refreshToken?: string
  user: UserInfo
}

export interface PageResp<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface PackageInfo {
  id: number | string
  code: string
  name: string
  speedMbps: number
  monthlyFee: number
  description?: string
}

export interface OrderSummary {
  id: number | string
  orderNo: string
  packageCode: string
  packageName: string
  installAddress: string
  status: string
  statusLabel: string
  resourceStatus?: string
  appointmentTime?: string
  completedTime?: string
  createTime?: string
  canResubmit?: boolean
  canReschedule?: boolean
  canEvaluate?: boolean
}

export interface TimelineItem {
  eventTime: string
  source: string
  eventType: string
  description: string
  operatorName?: string
}

export interface OrderDetail {
  order: OrderSummary & Record<string, unknown>
  customer?: Record<string, unknown>
  appointment?: { appointmentTime?: string; status?: string; remark?: string }
  timeline: TimelineItem[]
}

/** 客户履约进度（H5 5 节点骨架 + 催单状态） */
export interface CustomerTrackStage {
  code: string
  name: string
  /** DONE | CURRENT | PENDING | EXCEPTION | SKIP */
  state: 'DONE' | 'CURRENT' | 'PENDING' | 'EXCEPTION' | 'SKIP'
  time?: string | null
  remark?: string | null
}

export interface CustomerTrack {
  orderNo: string
  status: string
  statusLabel: string
  packageName: string
  stages: CustomerTrackStage[]
  progress: string
  canUrge: boolean
  urgeCoolDownSeconds: number
  urgeThresholdMinutes: number
  terminal: boolean
}

export interface ResourceCheckResult {
  status: 'RESOURCE_OK' | 'RESOURCE_INSUFFICIENT' | 'NO_COVERAGE'
  message: string
  roomId?: number | string
  communityName?: string
  buildingName?: string
  unitName?: string
  roomNo?: string
}

export interface ServiceTicket {
  id: number | string
  ticketNo: string
  orderId?: number | string
  type: 'REPAIR' | 'COMPLAINT'
  category?: string
  description: string
  attachments?: string
  status: string
  handleResult?: string
  createTime?: string
}

export interface Profile {
  account: { username: string; status: number; bindTime?: string; lastLoginTime?: string }
  customer: { id: number | string; name: string; phone: string; idCardNo?: string; address?: string }
}

export interface ProfileChange {
  id: number | string
  changeFields: string
  proposedName?: string
  proposedPhone?: string
  proposedIdCardNo?: string
  proposedAddress?: string
  status: string
  reviewRemark?: string
  createTime?: string
}

export interface MessageItem {
  id: number | string
  content?: string
  templateCode?: string
  status: string
  createTime?: string
}
