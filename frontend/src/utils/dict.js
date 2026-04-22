export const POST_TYPE_OPTIONS = [
  { label: '全部', value: '' },
  { label: '失物', value: 'LOST' },
  { label: '寻物', value: 'FOUND' },
]

export const CATEGORY_OPTIONS = [
  { label: '证件', value: 'CERTIFICATE' },
  { label: '数码', value: 'ELECTRONICS' },
  { label: '钥匙', value: 'KEY' },
  { label: '衣物', value: 'CLOTHING' },
  { label: '书籍', value: 'BOOK' },
  { label: '其他', value: 'OTHER' },
]

export const STATUS_OPTIONS = [
  { label: '进行中', value: 'OPEN' },
  { label: '已匹配', value: 'MATCHED' },
  { label: '已找回', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' },
]

export const USER_STATUS_OPTIONS = [
  { label: '正常', value: 'ACTIVE' },
  { label: '封禁', value: 'BANNED' },
]

export const POST_TYPE_LABEL = {
  LOST: '失物',
  FOUND: '寻物',
}

export const STATUS_LABEL = {
  OPEN: '进行中',
  MATCHED: '已匹配',
  RESOLVED: '已找回',
  CLOSED: '已关闭',
}

export const CATEGORY_LABEL = CATEGORY_OPTIONS.reduce((acc, item) => {
  acc[item.value] = item.label
  return acc
}, {})
