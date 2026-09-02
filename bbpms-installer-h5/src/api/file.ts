import { post } from './http'

export interface PresignResult {
  uploadUrl: string
  objectKey: string
  url: string
  expires: number
}

export function uploadFile(file: File | Blob, bizType: string): Promise<PresignResult> {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('bizType', bizType)
  return post<PresignResult>('/files/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
