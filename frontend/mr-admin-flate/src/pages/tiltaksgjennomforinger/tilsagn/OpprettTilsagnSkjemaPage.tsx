import { Heading } from "@navikt/ds-react";
import { useMatch, useParams } from "react-router-dom";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { useTiltaksgjennomforingById } from "../../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../../components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "../../../components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "../../../components/navigering/Brodsmuler";
import { SkjemaContainer } from "../../../components/skjema/SkjemaContainer";
import { SkjemaContent } from "../../../components/skjema/SkjemaContent";
import { OpprettTilsagnContainer } from "../../../components/tilsagn/OpprettTilsagnContainer";
import { ContainerLayout } from "../../../layouts/ContainerLayout";
import { inneholderUrl } from "../../../utils/Utils";
import { useGetTilsagnById } from "./useGetTilsagnById";

export function OpprettTilsagnSkjemaPage() {
  const { avtaleId } = useParams();
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: tilsagn } = useGetTilsagnById();
  const { data: saksbehandler } = useHentAnsatt();

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );
  const redigeringsModus = tilsagn && inneholderUrl(tilsagn.id);
  const godkjenningsModus = Boolean(tilsagn && tilsagn.opprettetAv !== saksbehandler?.navIdent);

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
            ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`
            : `/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Opprett tilsagn" : "Opprett tilsagn",
      lenke: redigeringsModus
        ? `/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}/opprett-tilsagn`
        : "/tiltaksgjennomforinger/opprett-tilsagn",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          {godkjenningsModus ? "Godkjenn tilsagn" : "Opprett tilsagn"}
        </Heading>
      </Header>
      <ContainerLayout>
        <SkjemaContainer>
          <SkjemaContent>
            {tiltaksgjennomforing ? (
              <OpprettTilsagnContainer
                tiltaksgjennomforing={tiltaksgjennomforing}
                tilsagnSkalGodkjennes={godkjenningsModus}
                tilsagn={tilsagn}
              />
            ) : null}
          </SkjemaContent>
        </SkjemaContainer>
      </ContainerLayout>
    </main>
  );
}
