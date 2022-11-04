import { Alert } from '@navikt/ds-react';
import ViewTiltaksgjennomforingDetaljer from '../tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer';

export function SanityPreview() {
  return (
    <>
      <Alert style={{ marginBottom: '2rem' }} variant="warning" data-testid="sanity-preview-alert">
        Forhåndsvisning av informasjon fra Sanity
      </Alert>
      <ViewTiltaksgjennomforingDetaljer />
    </>
  );
}
