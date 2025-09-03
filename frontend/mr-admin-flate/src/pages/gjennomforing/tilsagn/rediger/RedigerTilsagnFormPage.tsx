import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useTilsagn, useTilsagnRequest } from "../detaljer/tilsagnDetaljerLoader";
import { Laster } from "@/components/laster/Laster";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { useRequiredParams } from "@/hooks/useRequiredParams";

function useRedigerTilsagnFormData(gjennomforingId: string, tilsagnId: string) {
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: defaults } = useTilsagnRequest(tilsagnId);
  return { avtale, gjennomforing, defaults, opprettelse: tilsagnDetaljer.opprettelse };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId, tilsagnId } = useRequiredParams(["gjennomforingId", "tilsagnId"]);

  const { avtale, gjennomforing, defaults, opprettelse } = useRedigerTilsagnFormData(
    gjennomforingId,
    tilsagnId,
  );

  if (!avtale) {
    return <Laster tekst="Laster data..." />;
  }

  return (
    <>
      <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
      <TilsagnFormContainer avtale={avtale} gjennomforing={gjennomforing} defaults={defaults} />
    </>
  );
}
