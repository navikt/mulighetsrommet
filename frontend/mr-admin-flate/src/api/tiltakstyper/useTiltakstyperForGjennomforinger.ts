import { TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { erTilgjengeligForGjennomforinger } from "@/utils/tiltakstype";

export function useTiltakstyperForGjennomforinger(filter: TiltakstypeFilterType = {}) {
  const tiltakstyper = useTiltakstyper(filter);
  return tiltakstyper.filter(erTilgjengeligForGjennomforinger);
}
