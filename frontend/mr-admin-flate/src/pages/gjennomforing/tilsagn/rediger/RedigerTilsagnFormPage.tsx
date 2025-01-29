import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";
import { redigerTilsagnLoader } from "@/pages/gjennomforing/tilsagn/rediger/redigerTilsagnLoader";
import { TilsagnRequest } from "@mr/api-client-v2";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function RedigerTilsagnFormPage() {
  const { avtale, gjennomforing, tilsagn, godkjenteTilsagn } =
    useLoaderData<typeof redigerTilsagnLoader>();

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Rediger tilsagn",
    },
  ];

  const defaults: TilsagnRequest = {
    id: tilsagn.id,
    type: tilsagn.type,
    periodeStart: tilsagn.periodeStart,
    periodeSlutt: tilsagn.periodeSlutt,
    kostnadssted: tilsagn.kostnadssted.enhetsnummer,
    beregning: tilsagn.beregning.input,
    gjennomforingId: gjennomforing.id,
  };

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Rediger tilsagn
        </Heading>
      </Header>
      <ContentBox>
        <VStack gap={"8"}>
          <WhitePaddedBox>
            <TiltakDetaljerForTilsagn gjennomforing={gjennomforing} />
          </WhitePaddedBox>
          <WhitePaddedBox>
            <TilsagnFormContainer
              avtale={avtale}
              gjennomforing={gjennomforing}
              defaults={defaults}
            />
          </WhitePaddedBox>
          <WhitePaddedBox>
            <VStack gap="4">
              <Heading size="medium">Aktive tilsagn</Heading>
              {godkjenteTilsagn.length > 0 ? (
                <TilsagnTabell tilsagn={godkjenteTilsagn} />
              ) : (
                <Alert variant="info">Det finnes ingen tilsagn for dette tiltaket</Alert>
              )}
            </VStack>
          </WhitePaddedBox>
        </VStack>
      </ContentBox>
    </main>
  );
}
