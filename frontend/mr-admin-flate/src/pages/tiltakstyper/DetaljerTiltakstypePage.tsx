import { Alert, BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Laster } from "../../components/Laster";
import { TiltaksgjennomforingslisteForTiltakstyper } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingslisteForTiltakstyper";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";

interface Props {
  side: string;
}

export function DetaljerTiltakstypePage({ side }: Props) {
  const optionalTiltakstype = useTiltakstypeById();

  if (optionalTiltakstype.isFetching) {
    return <Laster tekst="Laster tiltakstype" />;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">
        <BodyShort>Klarte ikke finne tiltakstype</BodyShort>
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <>
      <Tilbakelenke>Tilbake til oversikt</Tilbakelenke>
      <Heading size="large" level="1">
        {tiltakstype.navn}
      </Heading>
      <dl>
        <dt>Tiltakskode:</dt>
        <dd>{tiltakstype.arenaKode}</dd>
      </dl>
      <TiltaksgjennomforingslisteForTiltakstyper
        tiltakstype={tiltakstype}
        side={side}
      />
    </>
  );
}
