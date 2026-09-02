import { get, post } from './http'
import type { InstallInfo, PhotoMeta, SignatureMeta } from '@/types/install'

export function arriveAtSite(
  orderId: string | number,
  payload: { lng: number; lat: number; address: string }
): Promise<void> {
  return post<void>(`/install/${orderId}/arrive`, payload)
}
export function saveInstallInfo(orderId: string | number, payload: InstallInfo): Promise<void> {
  return post<void>(`/install/${orderId}/info`, payload)
}
export function uploadPhoto(orderId: string | number, payload: PhotoMeta): Promise<void> {
  return post<void>(`/install/${orderId}/photos`, payload)
}
export function saveSignature(orderId: string | number, payload: SignatureMeta): Promise<{ objectKey: string }> {
  return post(`/install/${orderId}/signature`, payload)
}
/**
 * Backend InstallCompleteReq: {workOrderId, orderId, info, photos, signature, lat, lng, distance, remark}.
 * orderId is required by the BSS activation step.
 */
export function submitComplete(
  orderId: string | number,
  payload: {
    workOrderId: number
    orderId: number
    lng: number; lat: number; distance: number
    info: InstallInfo; photos: PhotoMeta[]; signature: SignatureMeta; remark?: string
  }
): Promise<void> {
  return post<void>(`/install/${orderId}/complete`, payload)
}
