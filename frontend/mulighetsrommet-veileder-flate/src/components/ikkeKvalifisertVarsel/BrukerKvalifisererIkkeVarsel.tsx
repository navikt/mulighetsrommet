import { Alert } from '@navikt/ds-react';
import { useBrukerHarRettPaaTiltak } from '../../hooks/useBrukerHarRettPaaTiltak';
import styles from './BrukerKvalifisererIkkeVarsel.module.scss';
import { Innsatsgruppe } from '../../../../mulighetsrommet-api-client/build/models/Innsatsgruppe';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';

export function BrukerKvalifisererIkkeVarsel() {
  const {
    brukerHarRettPaaTiltak,
    brukersInnsatsgruppe,
    innsatsgruppeForGjennomforing = Innsatsgruppe.STANDARD_INNSATS,
  } = useBrukerHarRettPaaTiltak();

  const brukerdata = useHentBrukerdata();

  return !brukerHarRettPaaTiltak && !brukerdata.data?.servicegruppe ? (
    <Alert variant="warning" className={styles.varsel}>
      Brukeren tilhører innsatsgruppen <code className={styles.code}>{brukersInnsatsgruppe?.replaceAll('_', ' ')}</code>
      , men tiltaksgjennomføringen gjelder for{' '}
      <code className={styles.code}>{innsatsgruppeForGjennomforing.replaceAll('_', ' ')}</code>.
    </Alert>
  ) : (
    <></>
  );
}
