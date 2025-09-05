import {
  compareAsc,
  isAfter,
  isBefore,
  isDate,
  isValid,
  lightFormat,
  parse,
  parseISO,
  ParseISOOptions,
  sub,
  Duration,
  max,
  add,
} from "date-fns";
import { tz } from "@date-fns/tz";
import { utc, UTCDate } from "@date-fns/utc";

const NORSK_TID = tz("Europe/Oslo");
const norwegianParseContext = { in: NORSK_TID };
const utcParseContext: ParseISOOptions<UTCDate> = { in: utc };

type UnparsedDate = string | Date | undefined | null;

/**
 * Format: "yyyy-MM-dd" hhvis gyldig dato
 * @param dato
 * @returns
 */
export function yyyyMMddFormatting(dato: UnparsedDate): string | undefined {
  const parsedDate = parseDate(dato);
  if (parsedDate) {
    return lightFormat(parsedDate, "yyyy-MM-dd");
  }
}

/**
 * Format: "dd.MM.yyyy" hhvis gyldig dato
 * @param dato
 * @returns
 */
export function formaterDato(dato: UnparsedDate): string | undefined {
  const parsedDato = parseDate(dato);
  if (!parsedDato) {
    return;
  }

  return parsedDato.toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

/**
 * Format: "dd.MM.yyyy hh:mm" hhvis gyldig dato
 * @param dato
 * @returns
 */
export function formaterDatoTid(dato: UnparsedDate): string | undefined {
  const parsedDato = parseDate(dato);
  if (!parsedDato) {
    return;
  }
  return parsedDato
    .toLocaleTimeString("no-NO", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
    .replace(",", " ");
}

type Periode = {
  start: string;
  slutt: string;
};

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
  return max(dates.map((it) => parseDate(it)).filter(isDate));
}

/**
 * Trekk fra gitt varighet for gitt dato, hhvis gyldig ellers undefined
 * @param dato
 * @param duration
 * @returns
 */
export function subDuration(dato: Date, duration: Duration): Date;
export function subDuration(dato: string | undefined | null, duration: Duration): Date | undefined;
export function subDuration(dato: UnparsedDate, duration: Duration) {
  const parsedDate = parseDate(dato);
  if (parsedDate) {
    return sub(parsedDate, duration);
  }
}

/**
 * Trekk fra gitt varighet for gitt dato, hhvis gyldig ellers undefined
 * @param dato
 * @param duration
 * @returns
 */
export function addDuration(dato: Date, duration: Duration): Date;
export function addDuration(dato: string | undefined | null, duration: Duration): Date | undefined;
export function addDuration(dato: UnparsedDate, duration: Duration): Date | undefined {
  const parsedDate = parseDate(dato);
  if (parsedDate) {
    return add(parsedDate, duration);
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
  const parsedDate = parseDate(dato);
  const parsedComparedDate = parseDate(sammenlignet);
  if (parsedDate && parsedComparedDate) {
    return compareAsc(parsedDate, parsedComparedDate) !== -1;
  }
  return false;
}

/**
 * Hvis **dato** er lik eller mellom **from**\/**to**, hhvis gyldige datoer
 * @param dato
 * @param param1
 * @returns
 */
export function inBetweenInclusive(
  dato: UnparsedDate,
  { from, to }: { from: UnparsedDate; to: UnparsedDate },
): boolean {
  const parsedDato = parseDate(dato);
  return isLaterOrSameDay(parsedDato, from) && isLaterOrSameDay(to, parsedDato);
}

/**
 * from > to = 1
 * from === to = 0
 * from < to = -1
 * @returns
 */
export function compare(from: UnparsedDate, to: UnparsedDate): number {
  const parsedFrom = parseDate(from);
  const parsedTo = parseDate(to);
  if (parsedFrom && parsedTo) {
    return compareAsc(parsedFrom, parsedTo);
  } else if (!parsedFrom && !parsedTo) {
    return 0;
  } else if (!parsedFrom) {
    return -1;
  } else {
    return 1;
  }
}
/**
 * **date** er strengt tidligere enn **compared** hhvis det er gyldige datoer, ellers false
 * @param date
 * @param compared
 * @returns
 */
export function isLater(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date);
  const parsedCompared = parseDate(compared);
  if (parsedDate && parsedCompared) {
    return isAfter(parsedDate, parsedCompared);
  }
  return false;
}

/**
 * **date** er strengt tidligere enn **compared** hhvis det er gyldige datoer, ellers false
 * @param date
 * @param compared
 * @returns
 */
export function isEarlier(date: UnparsedDate, compared: UnparsedDate): boolean {
  const parsedDate = parseDate(date);
  const parsedCompared = parseDate(compared);
  if (parsedDate && parsedCompared) {
    console.log(parsedDate?.toISOString(), parsedCompared?.toISOString());
    return isBefore(parsedDate, parsedCompared);
  }
  return false;
}

/**
 * date-fns allows years back to the start of the common era
 * The extra check for year is to not allow partial strings like '202' to be interpreted as year 202.
 */
const validateDate = (date: Date) => isValid(date) && date.getFullYear() > 1900;

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
    if (validateDate(date)) {
      return date;
    }
    return undefined;
  }

  const utcDate = parseISO(date, utcParseContext);
  if (validateDate(utcDate)) {
    return NORSK_TID(utcDate);
  }

  const localDate = parse(date, "dd.MM.yyyy HH:mm", new UTCDate(), norwegianParseContext);
  if (validateDate(localDate)) {
    return localDate;
  }

  const utcDate2 = parse(date, "dd.MM.yyyy", new UTCDate(), norwegianParseContext);
  if (validateDate(utcDate2)) {
    return utcDate2;
  }
  return undefined;
}
