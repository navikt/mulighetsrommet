import { compareAsc, isDate, isValid, lightFormat, parse, parseISO } from "date-fns"

type UnparsedDate = string | Date | undefined | null

export function formaterDatoSomYYYYMMDD(
  dato: UnparsedDate,
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
export function formaterDato(dato: UnparsedDate, fallback = ""): string {
  const parsedDato = parseDate(dato)
  if (!parsedDato) {
    return fallback
  }

  return parsedDato.toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

export function formaterDatoTid(dato: UnparsedDate, fallback = ""): string {
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
 * Sjekk om potensiell **dato** er senere enn **sammenlignet**\
 * Input blir forsøkt parset til date fra kjente dato format
 * @param dato
 * @param sammenlignet
 * @returns true om **dato** er større eller lik **sammenlignet**, ellers false
 */
export function isAfterOrSameDay(dato: UnparsedDate, sammenlignet: UnparsedDate): boolean {
  const parsedDate = parseDate(dato)
  const parsedComparedDate = parseDate(sammenlignet)
  if (parsedDate && parsedComparedDate) {
    return compareAsc(parsedDate,parsedComparedDate) !== -1
  }
  return false
}


export function inBetweenInclusive(dato: UnparsedDate, {fraDato, tilDato}: {fraDato: UnparsedDate, tilDato: UnparsedDate}): boolean {
  const parsedDato = parseDate(dato)
  return isAfterOrSameDay(parsedDato, fraDato) && isAfterOrSameDay(tilDato, parsedDato)
}

export function isAfter(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date)
  const parsedCompared = parseDate(compared)
  if (parsedDate && parsedCompared) {
    return isAfter(parsedDate, parsedCompared)
  }
  return false
}

export function isBefore(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date)
  const parsedCompared = parseDate(compared)
  if (parsedDate && parsedCompared) {
    return isBefore(parsedDate, parsedCompared)
  }
  return false
}

/**
 * Parse to known & valid date formats
 * @returns valid date, otherwise undefined
 */
export function parseDate(date: UnparsedDate): Date | undefined {
  if (!date) {
    return undefined;
  }

  if (isDate(date)) {
    if (isValid(date)) {
      return date
    }
    return undefined
  }

  let parsedDate = parseISO(date)
  if (isValid(parsedDate)) {
    return parsedDate
  }

  parsedDate = parse(date, "dd.MM.yyyy", new Date())
  if (isValid(parsedDate)) {
    return parsedDate
  }
  return undefined
}