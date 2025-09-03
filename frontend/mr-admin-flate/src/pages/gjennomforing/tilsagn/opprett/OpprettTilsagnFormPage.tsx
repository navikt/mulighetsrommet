import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { TilsagnBeregningType, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { useSearchParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useTilsagnDefaults } from "./opprettTilsagnLoader";
import { Laster } from "@/components/laster/Laster";
import { useRequiredParams } from "@/hooks/useRequiredParams";

function useHentData(gjennomforingId: string) {
  const [searchParams] = useSearchParams();
  const type = (searchParams.get("type") as TilsagnType | null) ?? TilsagnType.TILSAGN;
  const periodeStart = searchParams.get("periodeStart");
  const periodeSlutt = searchParams.get("periodeSlutt");
  const kostnadssted = searchParams.get("kostnadssted");

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
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

  return { gjennomforing, avtale, defaults };
}

export function OpprettTilsagnFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, avtale, defaults } = useHentData(gjennomforingId);

  if (!avtale) {
    return <Laster tekst="Laster data..." />;
  }

  return <TilsagnFormContainer avtale={avtale} gjennomforing={gjennomforing} defaults={defaults} />;
}
