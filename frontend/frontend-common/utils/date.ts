import { compareAsc, isAfter, isBefore, isDate, isValid, lightFormat, parse, parseISO, ParseISOOptions, sub, Duration, max } from "date-fns"
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
export function yyyyMMddFormatting(
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

type Periode = {
  start: string
  slutt: string
}

export function formaterPeriode(periode: Periode) {
  return `${formaterPeriodeStart(periode)} - ${formaterPeriodeSlutt(periode)}`;
}

export function formaterPeriodeStart({ start }: Periode) {
  return formaterDato(start);
}

export function formaterPeriodeSlutt({ slutt }: Periode) {
  return formaterDato(subDuration(slutt, { days: 1 }));
}

/**
 * Returnerer seneste datoen av listen, hhvis de er gyldige
 * @param dates 
 * @returns 
 */
export function maxOf(dates: UnparsedDate[]): Date {
  return max(dates.map((it) => parseDate(it)).filter(isDate))
}

/**
 * Trekk fra gitt varighet for gitt dato, hhvis gyldig ellers undefined
 * @param dato 
 * @param duration 
 * @returns 
 */
export function subDuration(dato: UnparsedDate, duration: Duration) {
  const parsedDate = parseDate(dato)
  if (parsedDate) {
    return sub(parsedDate,  duration)
  }
}

/**
 * Trekk fra gitt varighet for gitt dato, hhvis gyldig ellers undefined
 * @param dato 
 * @param duration 
 * @returns 
 */
export function addDuration(dato: UnparsedDate, duration: Duration) {
  const parsedDate = parseDate(dato)
  if (parsedDate) {
    return sub(parsedDate,  duration)
  }
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
 * from > to = 1
 * from === to = 0
 * from < to = -1
 * @returns 
 */
export function compare(from: UnparsedDate, to: UnparsedDate): number | undefined {
  const parsedFrom = parseDate(from)
  const parsedTo = parseDate(to)
  if (parsedFrom && parsedTo) {
    return compareAsc(parsedFrom, parsedTo)
  }
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
 * @returns valid date, ellers undefined
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

/* Tidligere: */

const ddMMyyyyFormat = new RegExp("^[0-9]{2}.[0-9]{2}.[0-9]{4}$");
const yyyyMMddFormat = new RegExp("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

/**
 * @deprecated Bruk yyyyMMddFormatting()
 */
export function formaterDatoSomYYYYMMDD(
  dato: string | Date | null | undefined,
  fallback = "",
): string {
  if (!dato) {
    return fallback;
  }

  if (typeof dato !== "string") {
    return dateToyyyyMMdd(dato, fallback);
  }

  if (yyyyMMddFormat.test(dato)) {
    const [year, month, day] = dato.split("-").map(Number);
    return dateToyyyyMMdd(new Date(year, month - 1, day), fallback)
  }

  if (ddMMyyyyFormat.test(dato)) {
    const [day, month, year] = dato.split(".").map(Number);
    return dateToyyyyMMdd(new Date(year, month - 1, day), fallback);
  }

  return fallback;
}

function dateToyyyyMMdd(dato: Date, fallback: string): string {
  if (isNaN(dato.getTime())) return fallback;

  const year = dato.getFullYear();
  const month = String(dato.getMonth() + 1).padStart(2, "0");
  const day = String(dato.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}