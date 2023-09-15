import { Alert, Loader } from '@navikt/ds-react';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useHentDeltMedBrukerStatus } from '../../core/api/queries/useHentDeltMedbrukerStatus';
import { useHentVeilederdata } from '../../core/api/queries/useHentVeilederdata';
import useTiltaksgjennomforingById from '../../core/api/queries/useTiltaksgjennomforingById';
import { useBrukerHarRettPaaTiltak } from '../../hooks/useBrukerHarRettPaaTiltak';
import { useFnr } from '../../hooks/useFnr';
import ViewTiltaksgjennomforingDetaljer from './ViewTiltaksgjennomforingDetaljer';
import styles from './ViewTiltaksgjennomforingDetaljer.module.scss';

export function ViewTiltaksgjennomforingDetaljerContainer() {
  const { data, isLoading, isError } = useTiltaksgjennomforingById();
  const fnr = useFnr();
  const { harDeltMedBruker } = useHentDeltMedBrukerStatus(data?.sanityId, fnr);
  const { brukerHarRettPaaTiltak, innsatsgruppeForGjennomforing } = useBrukerHarRettPaaTiltak();
  const veilederdata = useHentVeilederdata();
  const brukerdata = useHentBrukerdata();

  if (isLoading) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" />
      </div>
    );
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!data || !veilederdata?.data || !brukerdata?.data) return null;

  return (
    <ViewTiltaksgjennomforingDetaljer
      tiltaksgjennomforing={data}
      brukerHarRettPaaTiltak={brukerHarRettPaaTiltak}
      innsatsgruppeForGjennomforing={innsatsgruppeForGjennomforing}
      harDeltMedBruker={harDeltMedBruker}
      veilederdata={veilederdata.data}
      brukerdata={brukerdata.data}
    />
  );
}
