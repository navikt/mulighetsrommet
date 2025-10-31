import { Periode } from "@api-client";
import { isLater, isLaterOrSameDay } from "@mr/frontend-common/utils/date";

export function filtrerOverlappendePerioder<T extends { periode: Periode }>(
  valgtPeriode: Periode,
  liste: T[],
): T[] {
  return liste.filter(({ periode }) => {
    const innenforStart = overlapperStartAvPeriode(valgtPeriode, periode);
    const innenforSlutt = overlapperSluttAvPeriode(valgtPeriode, periode);
    const innenforPeriode = innenforValgtPeriode(valgtPeriode, periode);
    return innenforStart || innenforSlutt || innenforPeriode;
  });
}

export function overlapperStartAvPeriode(a: Periode, b: Periode): boolean {
  return isLaterOrSameDay(a.start, b.start) && isLater(b.slutt, a.start);
}

export function overlapperSluttAvPeriode(a: Periode, b: Periode): boolean {
  return isLater(a.slutt, b.start) && isLater(b.slutt, a.slutt);
}

export function innenforValgtPeriode(a: Periode, b: Periode): boolean {
  return isLater(b.start, a.start) && isLater(a.slutt, b.slutt);
}
