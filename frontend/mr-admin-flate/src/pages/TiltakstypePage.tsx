import { Alert } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../api/tiltakstyper/useTiltakstypeById";

export function TiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();

  if (optionalTiltakstype.isFetching) {
    return null;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">Klarte ikke finne tiltaksgjennomføring</Alert>
    );
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <div>
      <Link to="/tiltakstyper">Tilbake til oversikt</Link>
      <h1>{tiltakstype.navn}</h1>
      <dl>
        <dt>Tiltakskode:</dt>
        {/**
         * TODO Bytte ut med navn
         */}
        <dd>{tiltakstype.tiltakskode}</dd>
      </dl>

      {/**
       * TODO Implementere skjema for opprettelse av tiltakstype
       */}
    </div>
  );
}
