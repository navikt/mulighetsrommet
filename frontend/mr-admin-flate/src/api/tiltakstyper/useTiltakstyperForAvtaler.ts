import { TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { erTilgjengligForAvtaler } from "@/utils/tiltakstype";

export function useTiltakstyperForAvtaler(filter: TiltakstypeFilterType = {}) {
  const tiltakstyper = useTiltakstyper(filter);
  return tiltakstyper.filter(erTilgjengligForAvtaler);
}
