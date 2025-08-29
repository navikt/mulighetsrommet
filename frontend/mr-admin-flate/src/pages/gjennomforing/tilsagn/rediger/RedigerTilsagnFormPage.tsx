import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import {
  TilsagnBeregningRequest,
  TilsagnBeregningType,
  TilsagnDto,
  TilsagnRequest,
} from "@mr/api-client-v2";
import { Heading, VStack } from "@navikt/ds-react";
import { useParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useAktiveTilsagnTableData, useTilsagn } from "../detaljer/tilsagnDetaljerLoader";
import { Laster } from "@/components/laster/Laster";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { PiggybankFillIcon } from "@navikt/aksel-icons";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { AktiveTilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";

function useRedigerTilsagnFormData() {
  const { gjennomforingId, tilsagnId } = useParams();
  if (!gjennomforingId || !tilsagnId) {
    throw Error("Fant ikke gjennomforingId eller tilsagnId i url");
  }
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: aktiveTilsagn } = useAktiveTilsagnTableData(gjennomforingId);

  return {
    avtale,
    gjennomforing,
    ...tilsagnDetaljer,
    aktiveTilsagn,
  };
}

export function RedigerTilsagnFormPage() {
  const { gjennomforingId } = useParams();
  const { avtale, gjennomforing, aktiveTilsagn, tilsagn, opprettelse } =
    useRedigerTilsagnFormData();

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Rediger tilsagn",
    },
  ];

  if (!avtale) {
    return <Laster tekst="Laster data..." />;
  }

  const defaults: TilsagnRequest = {
    id: tilsagn.id,
    type: tilsagn.type,
    periodeStart: tilsagn.periode.start,
    periodeSlutt: yyyyMMddFormatting(subDuration(tilsagn.periode.slutt, { days: 1 }))!,
    kostnadssted: tilsagn.kostnadssted.enhetsnummer,
    beregning: tilsagnBeregningRequest(tilsagn),
    gjennomforingId: gjennomforing.id,
    kommentar: tilsagn.kommentar ?? undefined,
  };

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <PiggybankFillIcon color="#FFAA33" className="w-10 h-10" />
        <Heading size="large" level="2">
          Rediger tilsagn
        </Heading>
      </Header>
      <ContentBox>
        <VStack gap="4">
          <WhitePaddedBox>
            <VStack gap="6">
              <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
              <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
            </VStack>
            <TilsagnFormContainer
              avtale={avtale}
              gjennomforing={gjennomforing}
              defaults={defaults}
            />
          </WhitePaddedBox>
          <WhitePaddedBox>
            <VStack gap="4">
              <AktiveTilsagnTable data={aktiveTilsagn} />
            </VStack>
          </WhitePaddedBox>
        </VStack>
      </ContentBox>
    </>
  );
}

function tilsagnBeregningRequest(tilsagn: TilsagnDto): TilsagnBeregningRequest {
  const { beregning } = tilsagn;
  switch (beregning.type) {
    case "FRI":
      return {
        type: TilsagnBeregningType.FRI,
        linjer: beregning.linjer,
        prisbetingelser: beregning.prisbetingelser ?? undefined,
      };
    case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
      return {
        type: TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
      };
    case "PRIS_PER_MANEDSVERK":
      return {
        type: TilsagnBeregningType.PRIS_PER_MANEDSVERK,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
        prisbetingelser: beregning.prisbetingelser ?? undefined,
      };
    case "PRIS_PER_UKESVERK":
      return {
        type: TilsagnBeregningType.PRIS_PER_UKESVERK,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
        prisbetingelser: beregning.prisbetingelser ?? undefined,
      };
    case "PRIS_PER_TIME_OPPFOLGING":
      return {
        type: TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
        antallTimerOppfolgingPerDeltaker: beregning.antallTimerOppfolgingPerDeltaker,
        prisbetingelser: beregning.prisbetingelser ?? undefined,
      };
  }
}
