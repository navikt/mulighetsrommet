import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";
import { Kontorstruktur, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";

export function useRelevanteKostnadssteder(
  type: TilsagnType,
  kostnadssted: string | null,
  kontorstruktur: Kontorstruktur[],
): KostnadsstedOption[] {
  const { data: kostnadssteder } = useKostnadssteder();

  if (kostnadssted) {
    return kostnadssteder
      .filter(({ kostnadssteder }) => kostnadssteder.some((k) => k.enhetsnummer === kostnadssted))
      .flatMap(({ kostnadssteder }) => kostnadssteder);
  }

  const relevanteRegioner =
    type === TilsagnType.TILSAGN
      ? kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
      : kostnadssteder.map(({ region }) => region.enhetsnummer);

  return kostnadssteder
    .filter(({ region }) => relevanteRegioner.includes(region.enhetsnummer))
    .flatMap(({ kostnadssteder }) => kostnadssteder);
}
