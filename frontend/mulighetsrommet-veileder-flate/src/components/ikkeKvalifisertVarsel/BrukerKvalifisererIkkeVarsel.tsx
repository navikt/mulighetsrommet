import { Alert } from '@navikt/ds-react';
import { useBrukerHarRettPaaTiltak } from '../../hooks/useBrukerHarRettPaaTiltak';
import appStyles from '../../App.module.scss';
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

  return !brukerHarRettPaaTiltak && brukerdata.data?.innsatsgruppe ? (
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
