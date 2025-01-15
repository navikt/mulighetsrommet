import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useLoaderData, useMatch, useParams } from "react-router";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnSkjemaContainer } from "@/components/tilsagn/TilsagnSkjemaContainer";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";
import { redigerTilsagnLoader } from "@/pages/gjennomforing/tilsagn/rediger/redigerTilsagnLoader";
import { TilsagnRequest } from "@mr/api-client";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function RedigerTilsagnSkjemaPage() {
  const { avtaleId } = useParams();

  const { avtale, gjennomforing, tilsagn, godkjenteTilsagn } =
    useLoaderData<typeof redigerTilsagnLoader>();

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/gjennomforinger/:gjennomforingId/rediger-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtale",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/gjennomforinger`,
        }
      : undefined,
    {
      tittel: "Gjennomføring",
      lenke: avtaleId
        ? `/avtaler/${avtaleId}/gjennomforinger/${gjennomforing.id}`
        : `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Rediger tilsagn",
      lenke: `/gjennomforinger/${gjennomforing.id}/rediger-tilsagn`,
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
            <TilsagnSkjemaContainer
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
