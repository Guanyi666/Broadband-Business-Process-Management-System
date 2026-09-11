import { post } from './http'
import type { InstallInfo, PhotoMeta, SignatureMeta } from '@/types/install'

export function arriveAtSite(
  orderId: string | number,
  payload: { lng: number; lat: number; address: string }
): Promise<void> {
  return post<void>(`/install/${orderId}/arrive`, { ...payload, workOrderId: Number(orderId) })
}
export function saveInstallInfo(orderId: string | number, payload: InstallInfo): Promise<void> {
  return post<void>(`/install/${orderId}/info`, { ...payload, workOrderId: Number(orderId) })
}
export function uploadPhoto(orderId: string | number, payload: PhotoMeta): Promise<void> {
  return post<void>(`/install/${orderId}/photos`, {
    workOrderId: Number(orderId),
    objectKey: payload.objectKey
  })
}
export function saveSignature(orderId: string | number, payload: SignatureMeta): Promise<void> {
  return post<void>(`/install/${orderId}/signature`, {
    workOrderId: Number(orderId),
    customerName: payload.customerName,
    objectKey: payload.objectKey
  })
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
  const workOrderId = Number(orderId)
  return post<void>(`/install/${orderId}/complete`, {
    ...payload,
    workOrderId,
    info: { ...payload.info, workOrderId },
    photos: payload.photos.map((photo) => ({ workOrderId, objectKey: photo.objectKey })),
    signature: {
      workOrderId,
      customerName: payload.signature.customerName,
      objectKey: payload.signature.objectKey
    }
  })
}
