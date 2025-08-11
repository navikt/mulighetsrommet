import { formaterDato } from "@mr/frontend-common/utils/date";
import { ArrFlateUtbetaling } from "api-client";

/**
 * @deprecated Bruk `subDuration(date, {days: number})`
 */
export function subtractDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() - numDays);
  return newDate;
}

export function formaterFoedselsdato(foedselsdato: string | undefined, foedselsaar?: number) {
  return foedselsdato
    ? formaterDato(foedselsdato)
    : foedselsaar
      ? `Fødselsår: ${foedselsaar}`
      : null;
}

interface TimestampInfo {
  title: string;
  value: string;
}

export const getTimestamp = (utbetaling: ArrFlateUtbetaling): TimestampInfo => {
  if (utbetaling.godkjentAvArrangorTidspunkt) {
    return {
      title: "Dato innsendt",
      value: formaterDato(utbetaling.godkjentAvArrangorTidspunkt) ?? "-",
    };
  }

  return {
    title: "Dato opprettet hos Nav",
    value: formaterDato(utbetaling.createdAt) ?? "-",
  };
};
