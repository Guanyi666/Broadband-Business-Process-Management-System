import AMapLoader from '@amap/amap-jsapi-loader'
import { showToast } from 'vant'

export interface GeoPosition {
  lng: number
  lat: number
  accuracy: number
  address?: string
}

const AMap_KEY = import.meta.env.VITE_AMAP_KEY

export function haversineDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000
  const toRad = (d: number) => (d * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return Math.round(2 * R * Math.asin(Math.sqrt(a)))
}

export async function getCurrentPosition(): Promise<GeoPosition> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({
        lng: pos.coords.longitude,
        lat: pos.coords.latitude,
        accuracy: pos.coords.accuracy
      }),
      (err) => {
        showToast('请开启定位权限')
        reject(err)
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
    )
  })
}

export async function loadAMap(): Promise<unknown> {
  return AMapLoader.load({
    key: AMap_KEY,
    version: '2.0',
    plugins: ['AMap.Geolocation', 'AMap.Geocoder', 'AMap.Marker']
  })
}
