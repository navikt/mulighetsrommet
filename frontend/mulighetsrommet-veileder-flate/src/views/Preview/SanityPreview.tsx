import { Alert } from '@navikt/ds-react';
import ViewTiltakstypeDetaljer from '../tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';

export function SanityPreview() {
  return (
    <>
      <Alert variant="warning">Forhåndsvisning av informasjon fra Sanity</Alert>
      <ViewTiltakstypeDetaljer />
    </>
  );
}
