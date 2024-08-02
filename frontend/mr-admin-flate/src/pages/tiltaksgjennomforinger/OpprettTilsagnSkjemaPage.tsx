import { Heading } from "@navikt/ds-react";
import { TiltaksgjennomforingStatusTag } from "mulighetsrommet-frontend-common";
import { Header } from "../../components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "../../components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "../../components/navigering/Brodsmuler";
import { SkjemaContainer } from "../../components/skjema/SkjemaContainer";
import { SkjemaContent } from "../../components/skjema/SkjemaContent";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { useMatch, useParams } from "react-router-dom";
import { inneholderUrl } from "../../utils/Utils";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { OpprettTilsagnContainer } from "../../components/tilsagn/OpprettTilsagnContainer";

export function OpprettTilsagnSkjemaPage() {
  const { avtaleId } = useParams();
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();

  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/opprett-tilsagn",
  );
  const redigeringsModus = tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id);

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
          Opprett tilsagn
        </Heading>
        {tiltaksgjennomforing ? (
          <TiltaksgjennomforingStatusTag status={tiltaksgjennomforing.status} showAvbruttAarsak />
        ) : null}
      </Header>
      <ContainerLayout>
        <SkjemaContainer>
          <SkjemaContent>
            {tiltaksgjennomforing ? (
              <OpprettTilsagnContainer tiltaksgjennomforing={tiltaksgjennomforing} />
            ) : null}
          </SkjemaContent>
        </SkjemaContainer>
      </ContainerLayout>
    </main>
  );
}
