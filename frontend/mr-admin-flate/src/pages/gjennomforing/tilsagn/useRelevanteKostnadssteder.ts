import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";
import { useKostnadsstedFilter } from "@/api/enhet/useKostnadsstedFilter";
import { Kontorstruktur, TilsagnType } from "@tiltaksadministrasjon/api-client";

export function useRelevanteKostnadssteder(
  type: TilsagnType,
  kontorstruktur: Kontorstruktur[],
): KostnadsstedOption[] {
  const { data: kostnadssteder } = useKostnadsstedFilter();

  const relevanteRegioner =
    type === TilsagnType.TILSAGN
      ? kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
      : kostnadssteder.map((region) => region.enhetsnummer);

  return kostnadssteder
    .filter((region) => relevanteRegioner.includes(region.enhetsnummer))
    .flatMap((region) => region.enheter);
}
