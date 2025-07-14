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
 * Formatter gyldige datoer
 * @returns norsk dato format, ellers fallback (default "")
 */
export function formaterDato(dato: string | Date | undefined | null, fallback = ""): string {
  const parsedDato = parseDate(dato)
  if (!parsedDato) {
    return fallback
  }

  return new Date(parsedDato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

export function formaterDatoTid(dato: string | Date, fallback = ""): string {
  const parsedDato = parseDate(dato)
  if (!parsedDato) {
    return fallback
  }
  return parsedDato.toLocaleTimeString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).replace(",", " ")
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