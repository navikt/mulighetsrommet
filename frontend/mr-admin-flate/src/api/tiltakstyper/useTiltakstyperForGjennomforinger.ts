import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { erTilgjengeligForGjennomforinger } from "@/utils/tiltakstype";

export function useTiltakstyperForGjennomforinger() {
  const tiltakstyper = useTiltakstyper();
  return tiltakstyper.filter(erTilgjengeligForGjennomforinger);
}
