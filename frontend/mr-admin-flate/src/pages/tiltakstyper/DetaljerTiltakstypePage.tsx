import { Alert, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Laster } from "../../components/Laster";
import { TiltaksgjennomforingslisteForTiltakstyper } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingslisteForTiltakstyper";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";
import { useSideForNavigering } from "../../hooks/useSideForNavigering";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();
  const side = useSideForNavigering();

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
