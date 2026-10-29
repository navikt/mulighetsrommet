import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagn, useTilsagnRequest } from "../detaljer/tilsagnDetaljerLoader";
import { TotrinnskontrollReturnert } from "@/components/totrinnskontroll/TotrinnskontrollReturnert";
import { useRequiredParams } from "@/hooks/useRequiredParams";

function useRedigerTilsagnFormData(gjennomforingId: string, tilsagnId: string) {
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: defaults } = useTilsagnRequest(tilsagnId);
  return {
    gjennomforing: detaljer.gjennomforing,
    prismodell: detaljer.prismodell,
    defaults,
    opprettelse: tilsagnDetaljer.opprettelse,
  };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId, tilsagnId } = useRequiredParams(["gjennomforingId", "tilsagnId"]);

  const { gjennomforing, prismodell, defaults, opprettelse } = useRedigerTilsagnFormData(
    gjennomforingId,
    tilsagnId,
  );

  return (
    <>
      <TotrinnskontrollReturnert heading="Tilsagnet ble returnert" opprettelse={opprettelse} />
      <TilsagnFormContainer
        gjennomforing={gjennomforing}
        prismodell={prismodell}
        defaults={defaults}
      />
    </>
  );
}
