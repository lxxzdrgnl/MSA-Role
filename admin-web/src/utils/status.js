export const STATUS_MAP = {
  PENDING: '대기 중',
  ACCEPTED: '주문 확인',
  COOKING: '조리 중',
  READY: '완료',
  COMPLETED: '완료',
  CANCELLED: '취소됨',
}

export const CONGESTION_LABELS = { LOW: '여유', MEDIUM: '보통', HIGH: '혼잡' }

export function statusLabel(s) {
  return STATUS_MAP[s] || s
}

export function statusClass(s) {
  if (s === 'COMPLETED' || s === 'READY') return 'pill-done'
  if (s === 'CANCELLED') return 'pill-cancel'
  return 'pill-active'
}

export function nextStatus(s) {
  return { PENDING: 'ACCEPTED', ACCEPTED: 'COOKING', COOKING: 'COMPLETED' }[s] || null
}

export function nextStatusLabel(s) {
  return { PENDING: '주문 확인', ACCEPTED: '조리 시작', COOKING: '완료 처리' }[s] || ''
}

export function flowBtnClass(s) {
  return { PENDING: 'flow-accept', ACCEPTED: 'flow-cooking', COOKING: 'flow-complete' }[s] || ''
}

export function canCancel(s) {
  return s === 'PENDING' || s === 'COOKING'
}

export function congestionPercent(level) {
  return { LOW: 20, MEDIUM: 55, HIGH: 90 }[level] ?? 0
}
