import { ArrFlateUtbetaling, Periode } from "api-client";

export function formaterPeriode(periode: Periode) {
  const start = formaterDato(periode.start);
  const slutt = formaterDato(subtractDays(new Date(periode.slutt), 1));
  return `${start} - ${slutt}`;
}

export function formaterDato(dato: string | Date, fallback = ""): string {
  const result = new Date(dato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  if (result === "Invalid Date") {
    return fallback;
  }

  return result;
}

export function formaterDatoTid(dato: string | Date): string {
  if (!dato) return "";

  const result = new Date(dato).toLocaleString("no-NO", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

  return result.replace(",", " kl. ");
}

export function subtractDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() - numDays);
  return newDate;
}

interface TimestampInfo {
  title: string;
  value: string;
}

export const getTimestamp = (utbetaling: ArrFlateUtbetaling): TimestampInfo => {
  if (utbetaling.godkjentAvArrangorTidspunkt) {
    return {
      title: "Dato innsendt",
      value: formaterDato(utbetaling.godkjentAvArrangorTidspunkt),
    };
  }

  return {
    title: "Dato opprettet hos Nav",
    value: utbetaling.createdAt ? formaterDato(utbetaling.createdAt) : "-",
  };
};
