import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { TilsagnBeregningType, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { useSearchParams } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useTilsagnDefaults } from "./opprettTilsagnLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";

function useHentData(gjennomforingId: string) {
  const [searchParams] = useSearchParams();
  const type = (searchParams.get("type") as TilsagnType | null) ?? TilsagnType.TILSAGN;
  const periodeStart = searchParams.get("periodeStart");
  const periodeSlutt = searchParams.get("periodeSlutt");
  const kostnadssted = searchParams.get("kostnadssted");

  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: defaults } = useTilsagnDefaults({
    id: null,
    gjennomforingId,
    type,
    periodeStart: periodeStart,
    periodeSlutt: periodeSlutt,
    // Denne blir bestemt av backend men er påkrevd
    beregning: {
      type: TilsagnBeregningType.FRI,
      antallPlasser: null,
      prisbetingelser: null,
      antallTimerOppfolgingPerDeltaker: null,
      linjer: [],
    },
    kostnadssted: kostnadssted,
    kommentar: null,
  });

  return { gjennomforing, defaults };
}

export function OpprettTilsagnFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, defaults } = useHentData(gjennomforingId);

  return <TilsagnFormContainer gjennomforing={gjennomforing} defaults={defaults} />;
}
