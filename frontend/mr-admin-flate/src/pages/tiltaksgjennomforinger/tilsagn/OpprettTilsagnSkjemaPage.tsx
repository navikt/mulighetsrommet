import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useLoaderData, useMatch, useParams } from "react-router-dom";
import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { SkjemaContainer } from "@/components/skjema/SkjemaContainer";
import { SkjemaContent } from "@/components/skjema/SkjemaContent";
import { TilsagnSkjemaContainer } from "@/components/tilsagn/TilsagnSkjemaContainer";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { inneholderUrl } from "@/utils/Utils";
import { tilsagnLoader } from "./tilsagnLoader";
import { TilsagnTabell } from "./TilsagnTabell";
import { TiltakDetaljerForTilsagn } from "@/components/tilsagn/TiltakDetaljerForTilsagn";

export function OpprettTilsagnSkjemaPage() {
  const { avtaleId } = useParams();

  const { tiltaksgjennomforing, tilsagn, tilsagnForGjennomforing } =
    useLoaderData<typeof tilsagnLoader>();
  const aktiveTilsagn = tilsagnForGjennomforing?.filter((d) => d.status.type === "GODKJENT");

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );
  const redigeringsModus = tilsagn && inneholderUrl(tilsagn.id);

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
    redigeringsModus
      ? {
          tittel: "Tiltaksgjennomføringdetaljer",
          lenke: avtaleId
            ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`
            : `/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Opprett tilsagn" : "Opprett tilsagn",
      lenke: redigeringsModus
        ? `/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/opprett-tilsagn`
        : "/tiltaksgjennomforinger/opprett-tilsagn",
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
      <ContainerLayout>
        <VStack gap={"8"}>
          <SkjemaContainer>
            <SkjemaContent>
              <TiltakDetaljerForTilsagn tiltaksgjennomforing={tiltaksgjennomforing} />

              <TilsagnSkjemaContainer
                tiltaksgjennomforing={tiltaksgjennomforing}
                tilsagn={tilsagn}
              />
            </SkjemaContent>
          </SkjemaContainer>

          <SkjemaContainer>
            <SkjemaContent>
              <VStack gap="4">
                <Heading size="medium">Aktive tilsagn</Heading>
                {aktiveTilsagn && aktiveTilsagn.length > 0 ? (
                  <TilsagnTabell tilsagn={aktiveTilsagn} />
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
