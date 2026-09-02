export interface InstallInfo {
  onuMac: string
  onuSn?: string
  oltPort?: string
  oltName?: string
  signal: number
  bandwidth?: string
}

export interface PhotoMeta {
  objectKey: string
  url: string
  name?: string
  size?: number
  tags?: string[]
  capturedAt: number
}

export interface SignatureMeta {
  customerName: string
  dataUrl?: string
  objectKey: string
  capturedAt: number
}
