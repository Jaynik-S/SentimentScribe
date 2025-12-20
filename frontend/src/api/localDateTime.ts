const pad2 = (value: number): string => value.toString().padStart(2, '0')
const pad3 = (value: number): string => value.toString().padStart(3, '0')

type FormatOptions = {
  includeMilliseconds?: boolean
}

export const formatLocalDateTime = (date: Date, options: FormatOptions = {}): string => {
  const year = date.getFullYear()
  const month = pad2(date.getMonth() + 1)
  const day = pad2(date.getDate())
  const hours = pad2(date.getHours())
  const minutes = pad2(date.getMinutes())
  const seconds = pad2(date.getSeconds())
  const milliseconds = date.getMilliseconds()

  const fractional =
    options.includeMilliseconds && milliseconds > 0 ? `.${pad3(milliseconds)}` : ''

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}${fractional}`
}
