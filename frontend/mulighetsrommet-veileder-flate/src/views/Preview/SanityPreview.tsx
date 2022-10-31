import { Alert } from '@navikt/ds-react';
import ViewTiltaksgjennomforingDetaljer from '../tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer';

export function SanityPreview() {
  return (
    <>
      <Alert variant="warning">Forhåndsvisning av informasjon fra Sanity</Alert>
      <ViewTiltaksgjennomforingDetaljer />
    </>
  );
}
