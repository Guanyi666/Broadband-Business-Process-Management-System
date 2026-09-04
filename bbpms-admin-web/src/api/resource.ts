import request from '@/utils/request'

export interface CheckResult {
  status: 'RESOURCE_OK' | 'RESOURCE_INSUFFICIENT' | 'NO_COVERAGE'
  message: string
  communityId?: number
  communityName?: string
  buildingId?: number
  buildingName?: string
  unitId?: number
  unitName?: string
  roomId?: number
  roomNo?: string
}

export interface RegionItem {
  id: number
  name: string
  code: string
}

export interface CommunityItem {
  id: number
  regionId: number
  regionName?: string
  name: string
  address?: string
  gridCode?: string
}

export interface BuildingItem {
  id: number
  communityId: number
  communityName?: string
  name: string
  totalFloors?: number
}

export interface UnitItem {
  id: number
  buildingId: number
  name: string
}

export interface RoomItem {
  id: number
  unitId: number
  roomNo: string
  isInstalled: number
}

export interface OltItem {
  id: number
  name: string
  regionId: number
  regionName?: string
  ip?: string
  vendor?: string
  model?: string
  status?: number
}

export interface PonItem {
  id: number
  oltId: number
  oltName?: string
  name: string
  totalPorts: number
  usedPorts: number
}

export interface OnuItem {
  id: number
  roomId?: number
  roomNo?: string
  ponId?: number
  ponName?: string
  sn: string
  model?: string
  status: string
}

/** 资源核查（下单前） */
export function checkResource(address: string, roomNo?: string) {
  return request<CheckResult>({ url: '/resources/check', method: 'POST', data: { address, roomNo } })
}

export function listRegions() {
  return request<RegionItem[]>({ url: '/resources/regions', method: 'GET' })
}
export function createRegion(name: string, code: string) {
  return request<RegionItem>({ url: '/resources/regions', method: 'POST', params: { name, code } })
}
export function listCommunities(regionId?: number, name?: string) {
  return request<CommunityItem[]>({ url: '/resources/communities', method: 'GET', params: { regionId, name } })
}
export function createCommunity(regionId: number, name: string, address?: string) {
  return request<CommunityItem>({ url: '/resources/communities', method: 'POST', params: { regionId, name, address } })
}
export function listBuildings(communityId: number) {
  return request<BuildingItem[]>({ url: '/resources/buildings', method: 'GET', params: { communityId } })
}
export function createBuilding(communityId: number, name: string, totalFloors?: number) {
  return request<BuildingItem>({ url: '/resources/buildings', method: 'POST', params: { communityId, name, totalFloors } })
}
export function listUnits(buildingId: number) {
  return request<UnitItem[]>({ url: '/resources/units', method: 'GET', params: { buildingId } })
}
export function createUnit(buildingId: number, name: string) {
  return request<UnitItem>({ url: '/resources/units', method: 'POST', params: { buildingId, name } })
}
export function listRooms(unitId: number) {
  return request<RoomItem[]>({ url: '/resources/rooms', method: 'GET', params: { unitId } })
}
export function createRoom(unitId: number, roomNo: string) {
  return request<RoomItem>({ url: '/resources/rooms', method: 'POST', params: { unitId, roomNo } })
}
export function listOlts(regionId?: number) {
  return request<OltItem[]>({ url: '/resources/olts', method: 'GET', params: { regionId } })
}
export function createOlt(name: string, regionId: number, ip?: string, vendor?: string, model?: string) {
  return request<OltItem>({ url: '/resources/olts', method: 'POST', params: { name, regionId, ip, vendor, model } })
}
export function listPons(oltId: number) {
  return request<PonItem[]>({ url: '/resources/pons', method: 'GET', params: { oltId } })
}
export function createPon(oltId: number, name: string, totalPorts?: number) {
  return request<PonItem>({ url: '/resources/pons', method: 'POST', params: { oltId, name, totalPorts } })
}
export function listOnus(roomId?: number, status?: string) {
  return request<OnuItem[]>({ url: '/resources/onus', method: 'GET', params: { roomId, status } })
}
export function createOnu(sn: string, model?: string, roomId?: number) {
  return request<OnuItem>({ url: '/resources/onus', method: 'POST', params: { sn, model, roomId } })
}