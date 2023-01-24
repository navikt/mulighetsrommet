import { Alert, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Laster } from "../../components/Laster";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";
import { TiltaksgjennomforingslisteForTiltakstyper } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingslisteForTiltakstyper";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();

  if (optionalTiltakstype.isFetching) {
    return <Laster tekst="Laster tiltakstype" />;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltakstype
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <>
      <Tilbakelenke>Tilbake</Tilbakelenke>
      <Heading size="large" level="1">
        {tiltakstype.navn}
      </Heading>
      <dl>
        <dt>Tiltakskode:</dt>
        <dd>{tiltakstype.arenaKode}</dd>
      </dl>
      <TiltaksgjennomforingslisteForTiltakstyper tiltakstype={tiltakstype} />
    </>
  );
}
