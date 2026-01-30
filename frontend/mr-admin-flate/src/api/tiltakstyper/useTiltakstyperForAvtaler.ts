import { TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { kanOppretteAvtale } from "@/utils/tiltakstype";

export function useTiltakstyperForAvtaler(filter: TiltakstypeFilterType = {}) {
  const { data: tiltakstyper } = useTiltakstyper(filter);
  return tiltakstyper.filter(kanOppretteAvtale);
}
