import { formaterDato } from "@mr/frontend-common/utils/date";
import { ArrFlateUtbetaling, Periode } from "api-client";
import { subDays } from "date-fns"

export function formaterPeriode(periode: Periode) {
  const start = formaterDato(periode.start);
  const slutt = formaterDato(subDays(periode.slutt, 1));
  return `${start} - ${slutt}`;
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
      value: formaterDato(utbetaling.godkjentAvArrangorTidspunkt),
    };
  }

  return {
    title: "Dato opprettet hos Nav",
    value: utbetaling.createdAt ? formaterDato(utbetaling.createdAt) : "-",
  };
};
