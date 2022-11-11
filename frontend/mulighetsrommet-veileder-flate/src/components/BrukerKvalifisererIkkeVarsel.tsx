import { Alert, Heading } from '@navikt/ds-react';
import { Innsatsgruppe } from 'mulighetsrommet-api-client';
import { useBrukerHarRettPaaTiltak } from '../hooks/useUserHarRettPaaTiltak';

export function BrukerKvalifisererIkkeVarsel() {
  const {
    brukerHarRettPaaTiltak,
    brukersInnsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
    innsatsgruppeForGjennomforing = Innsatsgruppe.STANDARD_INNSATS,
  } = useBrukerHarRettPaaTiltak();

  if (!brukerHarRettPaaTiltak) {
    return (
      <Alert variant="warning">
        <Heading size="xsmall">Bruker kvalifiserer ikke til tiltaket</Heading>
        <p>
          Brukeren tilhører{' '}
          <code style={{ textTransform: 'lowercase' }}>{brukersInnsatsgruppe.replaceAll('_', ' ')}</code>, mens
          tiltaksgjennomføringen du er inne på gjelder for{' '}
          <code style={{ textTransform: 'lowercase' }}>{innsatsgruppeForGjennomforing.replaceAll('_', ' ')}</code>.
        </p>
      </Alert>
    );
  }

  return null;
}
