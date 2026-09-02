import request from '@/utils/request'

/** Backend FileUploadResp (com.bbpms.file.dto.FileUploadResp). */
export interface UploadResult {
  id: number | string
  objectKey?: string
  url: string
  thumbnailUrl?: string
  size: number
  contentType?: string
}

export function uploadFile(form: FormData, onProgress?: (e: ProgressEvent) => void) {
  return request<UploadResult>({
    url: '/files/upload',
    method: 'POST',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

/** Download presign for an existing attachment id (GET /files/{id}/presign). */
export function presign(id: number | string) {
  return request<{ url: string; expiresAt?: string }>({
    url: `/files/${id}/presign`,
    method: 'GET'
  })
}
