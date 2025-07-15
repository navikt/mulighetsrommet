import { compareAsc, isAfter, isBefore, isDate, isValid, lightFormat, parse, parseISO, ParseISOOptions } from "date-fns"
import { tz } from "@date-fns/tz"
import { utc, UTCDate } from "@date-fns/utc"

const NORSK_TID = tz("Europe/Oslo")
const norwegianParseContext = { in: NORSK_TID }
const utcParseContext: ParseISOOptions<UTCDate> = { in: utc }

type UnparsedDate = string | Date | undefined | null

/**
 * Format: "yyyy-MM-dd" hhvis gyldig dato, ellers fallback
 * @param dato 
 * @param fallback default ""
 * @returns 
 */
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
export function isLaterOrSameDay(dato: UnparsedDate, sammenlignet: UnparsedDate): boolean {
  const parsedDate = parseDate(dato)
  const parsedComparedDate = parseDate(sammenlignet)
  if (parsedDate && parsedComparedDate) {
    return compareAsc(parsedDate,parsedComparedDate) !== -1
  }
  return false
}

/**
 * Hvis **dato** er lik eller mellom **from**\/**to**, hhvis gyldige datoer
 * @param dato 
 * @param param1 
 * @returns 
 */
export function inBetweenInclusive(dato: UnparsedDate, {from, to}: {from: UnparsedDate, to: UnparsedDate}): boolean {
  const parsedDato = parseDate(dato)
  return isLaterOrSameDay(parsedDato, from) && isLaterOrSameDay(to, parsedDato)
}

/**
 * **date** er strengt tidligere enn **compared** hhvis det er gyldige datoer, ellers false
 * @param date 
 * @param compared 
 * @returns 
 */
export function isLater(date: UnparsedDate, compared: UnparsedDate): boolean {
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
export function isEarlier(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date)
  const parsedCompared = parseDate(compared)
  if (parsedDate && parsedCompared) {
    console.log(parsedDate?.toISOString(), parsedCompared?.toISOString())
    return isBefore(parsedDate, parsedCompared)
  }
  return false
}

/**
 * Parser til gyldige date objekter\
 * Støtter:
 *  - Gyldige **Date** objekter
 *  - ISO-8601 strenger (til UTC)
 *  - "dd.MM.yyyy" (til UTC)
 *  - "dd.MM.yyyy hh:mm" (til Europe/Oslo)
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

  const utcDate = parseISO(date, utcParseContext)
  if (isValid(utcDate)) {
    return utcDate
  }

  const localDate = parse(date, "dd.MM.yyyy HH:mm", new Date(), norwegianParseContext)
  if (isValid(localDate)) {
    return localDate
  }

  const utcDate2 = parse(date, "dd.MM.yyyy", new Date(), utcParseContext)
  if (isValid(utcDate2)) {
    return utcDate2
  }
  return undefined
}