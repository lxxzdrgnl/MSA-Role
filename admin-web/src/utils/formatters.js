export function formatPrice(v) {
  if (v == null) return '—'
  return Number(v).toLocaleString('ko-KR') + '원'
}

export function formatNum(v) {
  return v != null ? v.toLocaleString() : '—'
}

export function formatNumShort(v) {
  if (v >= 10000) return Math.round(v / 10000) + '만'
  if (v >= 1000) return Math.round(v / 1000) + '천'
  return String(v)
}

export function formatDate(v) {
  if (!v) return '—'
  return new Date(v).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function formatDateFull(v) {
  if (!v) return '—'
  return new Date(v).toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function timeAgo(v) {
  if (!v) return ''
  const mins = Math.floor((Date.now() - new Date(v).getTime()) / 60000)
  if (mins < 1) return '방금'
  if (mins < 60) return `${mins}분 전`
  return `${Math.floor(mins / 60)}시간 전`
}

export function summarizeItems(items) {
  if (!items || !items.length) return '—'
  const first = `${items[0].menuName} ×${items[0].quantity}`
  return items.length > 1 ? `${first} 외 ${items.length - 1}건` : first
}

export function extractListData(response) {
  const data = response.data
  if (Array.isArray(data)) return { list: data, totalPages: 1 }
  return {
    list: data.content || data.orders || data.menus || [],
    totalPages: data.totalPages || 1,
  }
}
