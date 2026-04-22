import dayjs from 'dayjs'

export const formatDateTime = (value, pattern = 'YYYY-MM-DD HH:mm:ss') => {
  if (!value) return '-'
  return dayjs(value).format(pattern)
}

export const toDateTimeParam = (value) => {
  if (!value) return null
  return dayjs(value).format('YYYY-MM-DDTHH:mm:ss')
}

export const truncate = (text, max = 30) => {
  if (!text) return ''
  return text.length > max ? `${text.slice(0, max)}...` : text
}
