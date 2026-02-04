import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { TilsagnBeregningType, TilsagnType, Valuta } from "@tiltaksadministrasjon/api-client";
import { useSearchParams } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagnDefaults } from "./opprettTilsagnLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { GjennomforingManglerPrismodellWarning } from "@/pages/gjennomforing/tilsagn/GjennomforingManglerPrismodellWarning";
import { useRelevanteKostnadssteder } from "@/pages/gjennomforing/tilsagn/useRelevanteKostnadssteder";

export function OpprettTilsagnFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell, kostnadssteder, defaults } = useHentData(gjennomforingId);

  if (!prismodell) {
    return <GjennomforingManglerPrismodellWarning />;
  }

  return (
    <TilsagnFormContainer
      gjennomforing={gjennomforing}
      prismodell={prismodell}
      kostnadssteder={kostnadssteder}
      defaults={defaults}
    />
  );
}

function useHentData(gjennomforingId: string) {
  const [searchParams] = useSearchParams();
  const type = (searchParams.get("type") as TilsagnType | null) ?? TilsagnType.TILSAGN;
  const periodeStart = searchParams.get("periodeStart");
  const periodeSlutt = searchParams.get("periodeSlutt");
  const kostnadssted = searchParams.get("kostnadssted");

  const { gjennomforing, veilederinfo, prismodell } = useGjennomforing(gjennomforingId);
  const { data: defaults } = useTilsagnDefaults({
    id: null,
    gjennomforingId,
    type,
    periodeStart: periodeStart,
    periodeSlutt: periodeSlutt,
    // Denne blir bestemt av backend men er påkrevd
    beregning: {
      type: TilsagnBeregningType.FRI,
      valuta: prismodell?.valuta ?? Valuta.NOK,
      antallPlasser: null,
      prisbetingelser: null,
      antallTimerOppfolgingPerDeltaker: null,
      linjer: [],
    },
    kostnadssted: kostnadssted || null,
    kommentar: null,
    beskrivelse: null,
  });

  const kostnadssteder = useRelevanteKostnadssteder(
    defaults.type,
    veilederinfo?.kontorstruktur ?? [],
  );
  return {
    gjennomforing,
    prismodell,
    kostnadssteder,
    defaults,
  };
}
