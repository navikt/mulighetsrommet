import { isDate, isValid, lightFormat, parse, parseISO } from "date-fns"

export function formaterDatoSomYYYYMMDD(
  dato: string | Date | null | undefined,
  fallback = "",
): string {
  const parsedDate = parseDate(dato)
  if (parsedDate) {
    return lightFormat(parsedDate, "yyyy-MM-dd");
  }
  return fallback;
}

/**
 * Parse to known & valid date formats
 * @returns valid date, otherwise null
 */
export function parseDate(date: string | Date | null | undefined): Date | null {
  if (!date) {
    return null;
  }

  if (isDate(date)) {
    if (isValid(date)) {
      return date
    }
    return null
  }

  let parsedDate = parseISO(date)
  if (isValid(parsedDate)) {
    return parsedDate
  }

  parsedDate = parse(date, "dd.MM.yyyy", new Date())
  if (isValid(parsedDate)) {
    return parsedDate
  }
  return null
}
