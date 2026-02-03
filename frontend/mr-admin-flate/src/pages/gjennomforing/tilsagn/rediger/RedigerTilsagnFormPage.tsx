import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagn, useTilsagnRequest } from "../detaljer/tilsagnDetaljerLoader";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { GjennomforingManglerPrismodellWarning } from "@/pages/gjennomforing/tilsagn/GjennomforingManglerPrismodellWarning";
import { useRelevanteKostnadssteder } from "@/pages/gjennomforing/tilsagn/useRelevanteKostnadssteder";

function useRedigerTilsagnFormData(gjennomforingId: string, tilsagnId: string) {
  const { gjennomforing, prismodell, veilederinfo } = useGjennomforing(gjennomforingId);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: defaults } = useTilsagnRequest(tilsagnId);
  const kostnadssteder = useRelevanteKostnadssteder(veilederinfo?.kontorstruktur ?? []);
  return {
    gjennomforing,
    prismodell,
    kostnadssteder,
    defaults,
    opprettelse: tilsagnDetaljer.opprettelse,
  };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId, tilsagnId } = useRequiredParams(["gjennomforingId", "tilsagnId"]);

  const { gjennomforing, prismodell, kostnadssteder, defaults, opprettelse } =
    useRedigerTilsagnFormData(gjennomforingId, tilsagnId);

  if (!prismodell) {
    return <GjennomforingManglerPrismodellWarning />;
  }

  return (
    <>
      <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
      <TilsagnFormContainer
        gjennomforing={gjennomforing}
        prismodell={prismodell}
        kostnadssteder={kostnadssteder}
        defaults={defaults}
      />
    </>
  );
}
