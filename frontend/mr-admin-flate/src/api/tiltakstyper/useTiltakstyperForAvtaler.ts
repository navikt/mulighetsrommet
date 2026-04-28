import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { erTilgjengligForAvtaler } from "@/utils/tiltakstype";

export function useTiltakstyperForAvtaler() {
  const tiltakstyper = useTiltakstyper();
  return tiltakstyper.filter(erTilgjengligForAvtaler);
}
