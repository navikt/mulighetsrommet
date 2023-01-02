import { Alert, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Laster } from "../../components/Laster";
import { TiltaksgjennomforingslisteForTiltakstyper } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingslisteForTiltakstyper";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();

  if (optionalTiltakstype.isFetching) {
    return <Laster tekst="Laster tiltakstype" />;
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
      <Link
        style={{ marginBottom: "1rem", display: "block" }}
        to="/tiltakstyper"
      >
        Tilbake til oversikt
      </Link>
      <Heading size="large" level="1">
        {tiltakstype.navn}
      </Heading>
      <dl>
        <dt>Tiltakskode:</dt>
        <dd>{tiltakstype.kode}</dd>
      </dl>
      <TiltaksgjennomforingslisteForTiltakstyper
        tiltakstype={tiltakstype}
      />
    </>
  );
}
