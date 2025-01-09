import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useLoaderData, useMatch, useParams } from "react-router";
import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TilsagnSkjemaContainer } from "@/components/tilsagn/TilsagnSkjemaContainer";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";
import { opprettTilsagnLoader } from "@/pages/tiltaksgjennomforinger/tilsagn/opprett/opprettTilsagnLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function OpprettTilsagnSkjemaPage() {
  const { avtaleId } = useParams();

  const { avtale, gjennomforing, defaults, godkjenteTilsagn } =
    useLoaderData<typeof opprettTilsagnLoader>();

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );

  const brodsmuler: Array<Brodsmule | undefined> = [
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtale",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Opprett tilsagn",
      lenke: "/tiltaksgjennomforinger/opprett-tilsagn",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          Opprett tilsagn
        </Heading>
      </Header>
      <ContentBox>
        <VStack gap={"8"}>
          <WhitePaddedBox>
            <TiltakDetaljerForTilsagn tiltaksgjennomforing={gjennomforing} />
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
