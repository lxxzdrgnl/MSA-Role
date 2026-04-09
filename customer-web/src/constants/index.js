export const STATUS_MAP = {
  PENDING:   { label: '접수 대기', tone: 'default' },
  ACCEPTED:  { label: '주문 접수', tone: 'default' },
  COOKING:   { label: '조리 중',   tone: 'accent' },
  PREPARING: { label: '조리 중',   tone: 'accent' },
  READY:     { label: '준비 완료', tone: 'accent' },
  COMPLETED: { label: '수령 완료', tone: 'muted' },
  CANCELLED: { label: '주문 취소', tone: 'danger' },
}

export const STATUS_LABELS = Object.fromEntries(
  Object.entries(STATUS_MAP).map(([k, v]) => [k, v.label])
)

export const CATEGORY_EMOJI = {
  '한식': '🥢',
  '중식': '🥡',
  '일식': '🍣',
  '분식': '🍢',
  '음료': '🧋',
  '디저트': '🍰',
}

export function categoryEmoji(name) {
  return CATEGORY_EMOJI[name] || '🍴'
}

export const REVIEW_KEYWORDS = [
  '맛있다', '양이 많다', '빠르다', '친절하다',
  '국물이 좋다', '매콤하다', '신선하다', '가성비 좋다'
]

export const QUICK_CHAT_QUERIES = ['매운 거 추천', '가벼운 식사', '인기 메뉴', '음료 추천']
