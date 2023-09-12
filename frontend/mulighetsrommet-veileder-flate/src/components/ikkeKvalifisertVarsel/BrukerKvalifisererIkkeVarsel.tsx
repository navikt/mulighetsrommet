import { Alert } from '@navikt/ds-react';
import { Bruker, Innsatsgruppe } from 'mulighetsrommet-api-client';
import appStyles from '../../App.module.scss';
import styles from './BrukerKvalifisererIkkeVarsel.module.scss';

interface Props {
  brukerdata?: Bruker;
  brukerHarRettPaaTiltak: boolean;
  brukersInnsatsgruppe?: Innsatsgruppe;
  innsatsgruppeForGjennomforing: Innsatsgruppe;
}

export function BrukerKvalifisererIkkeVarsel({
  brukerdata,
  brukerHarRettPaaTiltak,
  brukersInnsatsgruppe,
  innsatsgruppeForGjennomforing = Innsatsgruppe.STANDARD_INNSATS,
}: Props) {
  return !brukerHarRettPaaTiltak && brukerdata?.innsatsgruppe ? (
    <Alert variant="warning" className={styles.varsel}>
      Brukeren tilhører innsatsgruppen{' '}
      <strong className={appStyles.lowercase}>{brukersInnsatsgruppe?.replaceAll('_', ' ')}</strong>, men
      tiltaksgjennomføringen gjelder for{' '}
      <strong className={appStyles.lowercase}>{innsatsgruppeForGjennomforing.replaceAll('_', ' ')}</strong>.
    </Alert>
  ) : (
    <></>
  );
}
