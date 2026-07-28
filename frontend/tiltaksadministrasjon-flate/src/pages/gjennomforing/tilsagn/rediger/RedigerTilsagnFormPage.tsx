import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagn, useTilsagnRequest } from "../detaljer/tilsagnDetaljerLoader";
import { ToTrinnsOpprettelseForklaring } from "@/components/totrinnskontroll/ToTrinnskontrollOpprettelseForklaring";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useRelevanteKostnadssteder } from "@/pages/gjennomforing/tilsagn/useRelevanteKostnadssteder";

function useRedigerTilsagnFormData(gjennomforingId: string, tilsagnId: string) {
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: defaults } = useTilsagnRequest(tilsagnId);
  const kostnadssteder = useRelevanteKostnadssteder(
    tilsagnDetaljer.tilsagn.type,
    "ansvarligEnhet" in detaljer.gjennomforing
      ? detaljer.gjennomforing.ansvarligEnhet.enhetsnummer
      : null,
    "veilederinfo" in detaljer ? detaljer.veilederinfo.kontorstruktur : [],
  );
  return {
    gjennomforing: detaljer.gjennomforing,
    prismodell: detaljer.prismodell,
    kostnadssteder,
    defaults,
    opprettelse: tilsagnDetaljer.opprettelse,
  };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId, tilsagnId } = useRequiredParams(["gjennomforingId", "tilsagnId"]);

  const { gjennomforing, prismodell, kostnadssteder, defaults, opprettelse } =
    useRedigerTilsagnFormData(gjennomforingId, tilsagnId);

  return (
    <>
      <ToTrinnsOpprettelseForklaring heading="Tilsagnet ble returnert" opprettelse={opprettelse} />
      <TilsagnFormContainer
        gjennomforing={gjennomforing}
        prismodell={prismodell}
        kostnadssteder={kostnadssteder}
        defaults={defaults}
      />
    </>
  );
}
