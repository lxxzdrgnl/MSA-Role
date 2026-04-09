export function useFormatting() {
  const formatPrice = (p) => Number(p).toLocaleString('ko-KR')

  const formatDate = (s, opts = { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) => {
    if (!s) return ''
    return new Date(s).toLocaleString('ko-KR', opts)
  }

  const formatDateShort = (s) => formatDate(s, { month: '2-digit', day: '2-digit' })

  return { formatPrice, formatDate, formatDateShort }
}
