import { Alert, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../api/tiltakstyper/useTiltakstypeById";
import { TiltaksgjennomforingslisteForTiltakstyper } from "../components/tiltaksgjennomforinger/TiltaksgjennomforingslisteForTiltakstyper";

export function TiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();

  if (optionalTiltakstype.isFetching) {
    return null;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">
        <p>Klarte ikke finne tiltakstype</p>
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <>
      <Link to="/tiltakstyper">Tilbake til oversikt</Link>
      <Heading size="large" level="1">
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
