import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useLoaderData, useMatch, useParams } from "react-router-dom";
import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { SkjemaContainer } from "@/components/skjema/SkjemaContainer";
import { SkjemaContent } from "@/components/skjema/SkjemaContent";
import { TilsagnSkjemaContainer } from "@/components/tilsagn/TilsagnSkjemaContainer";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";
import { redigerTilsagnLoader } from "@/pages/tiltaksgjennomforinger/tilsagn/rediger/redigerTilsagnLoader";
import { TilsagnRequest } from "@mr/api-client";

export function RedigerTilsagnSkjemaPage() {
  const { avtaleId } = useParams();

  const { gjennomforing, tilsagn, godkjenteTilsagn } = useLoaderData<typeof redigerTilsagnLoader>();

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/rediger-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtaledetaljer",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Tiltaksgjennomføringdetaljer",
      lenke: avtaleId
        ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${gjennomforing.id}`
        : `/tiltaksgjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Rediger tilsagn",
      lenke: `/tiltaksgjennomforinger/${gjennomforing.id}/rediger-tilsagn`,
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
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          Rediger tilsagn
        </Heading>
      </Header>

      <ContainerLayout>
        <VStack gap={"8"}>
          <SkjemaContainer>
            <SkjemaContent>
              <TiltakDetaljerForTilsagn tiltaksgjennomforing={gjennomforing} />

              <TilsagnSkjemaContainer tiltaksgjennomforing={gjennomforing} defaults={defaults} />
            </SkjemaContent>
          </SkjemaContainer>

          <SkjemaContainer>
            <SkjemaContent>
              <VStack gap="4">
                <Heading size="medium">Aktive tilsagn</Heading>
                {godkjenteTilsagn.length > 0 ? (
                  <TilsagnTabell tilsagn={godkjenteTilsagn} />
                ) : (
                  <Alert variant="info">Det finnes ingen tilsagn for dette tiltaket</Alert>
                )}
              </VStack>
            </SkjemaContent>
          </SkjemaContainer>
        </VStack>
      </ContainerLayout>
    </main>
  );
}