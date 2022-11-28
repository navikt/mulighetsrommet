import { Alert } from '@navikt/ds-react';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import styles from './BrukerKvalifisererIkkeVarsel.module.scss';

export function BrukerHarIkke14aVedtakVarsel() {
  const brukerdata = useHentBrukerdata();

  return !brukerdata.data?.innsatsgruppe && brukerdata.data?.servicegruppe ? (
    <Alert variant="warning" className={styles.varsel}>
      Brukeren har ikke fått §14 a-vedtak enda, og kan derfor ikke meldes på noen tiltak.
    </Alert>
  ) : (
    <></>
  );
}
