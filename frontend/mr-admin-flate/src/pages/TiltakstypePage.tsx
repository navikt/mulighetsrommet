import { Alert, Heading } from "@navikt/ds-react";
import { useTiltakstypeById } from "../api/tiltakstyper/useTiltakstypeById";
import Tilbakeknapp from "mulighetsrommet-veileder-flate/src/components/tilbakeknapp/Tilbakeknapp";
import { TiltaksgjennomforingslisteForTiltakstyper } from "../components/tiltaksgjennomforinger/TiltaksgjennomforingslisteForTiltakstyper";

export function TiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();

  if (optionalTiltakstype.isFetching) {
    return null;
  }

  if (!optionalTiltakstype.data) {
    return <Alert variant="warning">Klarte ikke finne tiltakstype</Alert>;
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <>
      <Tilbakeknapp tilbakelenke="/oversikt" tekst="Tilbake til oversikt" />
      <Heading size="xlarge" level="1">
        {tiltakstype.navn}
      </Heading>
      <dl>
        <dt>Tiltakskode:</dt>
        {/**
         * TODO Bytte ut med navn
         */}
        <dd>{tiltakstype.tiltakskode}</dd>
      </dl>
      <TiltaksgjennomforingslisteForTiltakstyper
        tiltakstypeKode={tiltakstype.tiltakskode}
      />

      {/**
       * TODO Implementere skjema for opprettelse av tiltakstype
       */}
    </>
  );
}
