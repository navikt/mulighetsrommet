import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagn, useTilsagnRequest } from "../detaljer/tilsagnDetaljerLoader";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { useRequiredParams } from "@/hooks/useRequiredParams";

function useRedigerTilsagnFormData(gjennomforingId: string, tilsagnId: string) {
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: defaults } = useTilsagnRequest(tilsagnId);
  return { gjennomforing, defaults, opprettelse: tilsagnDetaljer.opprettelse };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId, tilsagnId } = useRequiredParams(["gjennomforingId", "tilsagnId"]);

  const { gjennomforing, defaults, opprettelse } = useRedigerTilsagnFormData(
    gjennomforingId,
    tilsagnId,
  );

  return (
    <>
      <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
      <TilsagnFormContainer gjennomforing={gjennomforing} defaults={defaults} />
    </>
  );
}
