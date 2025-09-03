import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnBeregningType, TilsagnType } from "@tiltaksadministrasjon/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import { useSearchParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useTilsagnDefaults } from "./opprettTilsagnLoader";
import { Laster } from "@/components/laster/Laster";
import { PiggybankFillIcon } from "@navikt/aksel-icons";
import { AktiveTilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
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

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: "/gjennomforinger",
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Opprett tilsagn",
    },
  ];

  if (!avtale) {
    return <Laster tekst="Laster data..." />;
  }

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <PiggybankFillIcon color="#FFAA33" className="w-10 h-10" />
        <Heading size="large" level="2">
          Opprett tilsagn
        </Heading>
      </Header>
      <ContentBox>
        <VStack gap="6">
          <WhitePaddedBox>
            <VStack gap="6">
              <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
              <TilsagnFormContainer
                avtale={avtale}
                gjennomforing={gjennomforing}
                defaults={defaults}
              />
            </VStack>
          </WhitePaddedBox>
        </VStack>
      </ContentBox>
      <WhitePaddedBox>
        <VStack gap="4">
          <AktiveTilsagnTable gjennomforingId={gjennomforingId} />
        </VStack>
      </WhitePaddedBox>
    </>
  );
}
