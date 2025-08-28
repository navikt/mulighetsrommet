import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnBeregningInput, TilsagnDto, TilsagnRequest } from "@mr/api-client-v2";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useAktiveTilsagn, useTilsagn } from "../detaljer/tilsagnDetaljerLoader";
import { Laster } from "@/components/laster/Laster";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { PiggybankFillIcon } from "@navikt/aksel-icons";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { TilsagnTable } from "../tabell/TilsagnTable";

function useRedigerTilsagnFormData() {
  const { gjennomforingId, tilsagnId } = useParams();
  if (!gjennomforingId || !tilsagnId) {
    throw Error("Fant ikke gjennomforingId eller tilsagnId i url");
  }
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: aktiveTilsagn } = useAktiveTilsagn(gjennomforingId);

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
    beregning: tilsagnBeregningInput(tilsagn),
    gjennomforingId: gjennomforing.id,
    kommentar: tilsagn.kommentar,
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
              <Heading size="medium">Aktive tilsagn</Heading>
              {aktiveTilsagn.length > 0 ? (
                <TilsagnTable tilsagn={aktiveTilsagn} />
              ) : (
                <Alert variant="info">
                  Det finnes ikke flere aktive tilsagn for dette tiltaket i Nav
                  Tiltaksadministrasjon
                </Alert>
              )}
            </VStack>
          </WhitePaddedBox>
        </VStack>
      </ContentBox>
    </>
  );
}

function tilsagnBeregningInput(tilsagn: TilsagnDto): TilsagnBeregningInput {
  const { periode, beregning } = tilsagn;
  switch (beregning.type) {
    case "FRI":
      return {
        type: "FRI",
        linjer: beregning.linjer,
        prisbetingelser: beregning.prisbetingelser,
      };
    case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
      return {
        type: "FAST_SATS_PER_TILTAKSPLASS_PER_MANED",
        periode,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
      };
    case "PRIS_PER_MANEDSVERK":
      return {
        type: "PRIS_PER_MANEDSVERK",
        periode,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
        prisbetingelser: beregning.prisbetingelser,
      };
    case "PRIS_PER_UKESVERK":
      return {
        type: "PRIS_PER_UKESVERK",
        periode,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
        prisbetingelser: beregning.prisbetingelser,
      };
    case "PRIS_PER_TIME_OPPFOLGING":
      return {
        type: "PRIS_PER_TIME_OPPFOLGING",
        periode,
        sats: beregning.sats,
        antallPlasser: beregning.antallPlasser,
        antallTimerOppfolgingPerDeltaker: beregning.antallTimerOppfolgingPerDeltaker,
        prisbetingelser: beregning.prisbetingelser,
      };
  }
}
