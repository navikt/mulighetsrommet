import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnType } from "@mr/api-client-v2";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useParams, useSearchParams } from "react-router";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useTilsagnDefaults } from "./opprettTilsagnLoader";
import { Laster } from "@/components/laster/Laster";
import { useAktiveTilsagn } from "../detaljer/tilsagnDetaljerLoader";
import { PiggybankFillIcon } from "@navikt/aksel-icons";
import { TilsagnTable } from "../tabell/TilsagnTable";

function useHentData() {
  const [searchParams] = useSearchParams();
  const type = (searchParams.get("type") as TilsagnType | null) ?? TilsagnType.TILSAGN;
  const periodeStart = searchParams.get("periodeStart");
  const periodeSlutt = searchParams.get("periodeSlutt");
  const kostnadssted = searchParams.get("kostnadssted");

  const { gjennomforingId } = useParams();
  if (!gjennomforingId) {
    throw Error("Fant ikke gjennomforingId i url");
  }

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: defaults } = useTilsagnDefaults({
    id: undefined,
    gjennomforingId,
    type,
    periodeStart: periodeStart ?? undefined,
    periodeSlutt: periodeSlutt ?? undefined,
    beregning: undefined,
    kostnadssted: kostnadssted ?? undefined,
  });

  const { data: aktiveTilsagn } = useAktiveTilsagn(gjennomforingId);

  return { gjennomforing, avtale, defaults, aktiveTilsagn };
}

export function OpprettTilsagnFormPage() {
  const { gjennomforingId } = useParams();
  const { gjennomforing, avtale, defaults, aktiveTilsagn } = useHentData();

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
          <Heading size="medium">Aktive tilsagn</Heading>
          {aktiveTilsagn.length > 0 ? (
            <TilsagnTable tilsagn={aktiveTilsagn} />
          ) : (
            <Alert variant="info">Det finnes ingen aktive tilsagn for dette tiltaket</Alert>
          )}
        </VStack>
      </WhitePaddedBox>
    </>
  );
}
