import { Periode } from "@arrangor-utbetalinger/api-client";
import { isLater } from "@mr/frontend-common/utils/date";

export function filtrerOverlappendePerioder<T extends { periode: Periode }>(
  valgtPeriode: Periode,
  liste: T[],
): T[] {
  return liste.filter(({ periode }) => overlapperPeriode(valgtPeriode, periode));
}

export function overlapperPeriode(a: Periode, b: Periode): boolean {
  return isLater(b.slutt, a.start) && isLater(a.slutt, b.start);
}
