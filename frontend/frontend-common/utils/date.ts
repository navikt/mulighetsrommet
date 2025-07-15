import { compareAsc, isDate, isValid, lightFormat, parse, parseISO } from "date-fns"

type UnparsedDate = string | Date | undefined | null

/**
 * Format: "yyyy-MM-dd" hhvis gyldig dato, ellers fallback
 * @param dato 
 * @param fallback default ""
 * @returns 
 */
export function isoDateStringFormat(
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
 * Format: "dd.MM.yyyy" hhvis gyldig dato, ellers fallback
 * @param dato 
 * @param fallback default ""
 * @returns 
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

/**
 * Format: "dd.MM.yyyy hh:mm" hhvis gyldig dato, ellers fallback
 * @param dato 
 * @param fallback default ""
 * @returns 
 */
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

/**
 * **date** er strengt tidligere enn **compared** hhvis det er gyldige datoer, ellers false
 * @param date 
 * @param compared 
 * @returns 
 */
export function isAfter(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date)
  const parsedCompared = parseDate(compared)
  if (parsedDate && parsedCompared) {
    return isAfter(parsedDate, parsedCompared)
  }
  return false
}

/**
 * **date** er strengt tidligere enn **compared** hhvis det er gyldige datoer, ellers false
 * @param date 
 * @param compared 
 * @returns 
 */
export function isBefore(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date)
  const parsedCompared = parseDate(compared)
  if (parsedDate && parsedCompared) {
    return isBefore(parsedDate, parsedCompared)
  }
  return false
}

/**
 * Parser til gyldige date objekter\
 * Støtter:
 *  - Gyldige **Date** objekter
 *  - ISO-8601 strenger
 *  - "dd.MM.yyyy"
 *  - "dd.MM.yyyy hh:mm"
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

  parsedDate = parse(date, "dd.MM.yyyy HH:mm", new Date())
  if (isValid(parsedDate)) {
    return parsedDate
  }

  parsedDate = parse(date, "dd.MM.yyyy", new Date())
  if (isValid(parsedDate)) {
    return parsedDate
  }
  return undefined
}