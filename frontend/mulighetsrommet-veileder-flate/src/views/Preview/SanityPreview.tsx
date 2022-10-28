import { useFeatureToggles } from '../../core/api/feature-toggles';
import TiltaksgjennomforingsHeader from '../../layouts/TiltaksgjennomforingsHeader';
import Nokkelinfo from '../../components/nokkelinfo/Nokkelinfo';
import SidemenyDetaljer from '../../components/sidemeny/SidemenyDetaljer';
import { Alert, Button, Loader } from '@navikt/ds-react';
import TiltaksdetaljerFane from '../../components/tabs/TiltaksdetaljerFane';
import Delemodal, { logDelMedbrukerEvent } from '../../components/modal/delemodal/Delemodal';
import useTiltaksgjennomforingByTiltaksnummer from '../../core/api/queries/useTiltaksgjennomforingByTiltaksnummer';
import { Tiltakstyper } from '../../core/api/models';
import { lenkeTilOpprettAvtaleForEnv } from '../CommonViews';
import { useState } from 'react';
import { useGetTiltaksgjennomforingIdFraUrl } from '../../core/api/queries/useGetTiltaksgjennomforingIdFraUrl';
import styles from '../tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer.module.scss';

export function SanityPreview() {
  const features = useFeatureToggles();
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingByTiltaksnummer();
  const whiteListOpprettAvtaleKnapp: Tiltakstyper[] = ['Midlertidig lønnstilskudd'];
  const [delemodalApen, setDelemodalApen] = useState<boolean>(false);
  const tiltaksnummer = useGetTiltaksgjennomforingIdFraUrl();

  if (features.data && !features.data['mulighetsrommet.enable-previewflate']) {
    return <p>Preview-flate er ikke aktivert enda</p>;
  }

  if (isLoading) {
    return <Loader className={styles.filter_loader} size="xlarge" />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med tiltaksnummer "${tiltaksnummer}"`}</Alert>
    );
  }

  const handleClickApneModal = () => {
    setDelemodalApen(true);
    logDelMedbrukerEvent('Åpnet dialog');
  };

  return (
    <>
      <Alert variant="warning">Forhåndsvisning av informasjon fra Sanity</Alert>

      <div className={styles.tiltakstypeDetaljer}>
        <div>
          <TiltaksgjennomforingsHeader />
          {tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter && (
            <Nokkelinfo nokkelinfoKomponenter={tiltaksgjennomforing.tiltakstype.nokkelinfoKomponenter} />
          )}
        </div>

        <div className={styles.sidemeny}>
          <SidemenyDetaljer />
          {whiteListOpprettAvtaleKnapp.includes(tiltaksgjennomforing.tiltakstype.tiltakstypeNavn) && (
            <Button
              as="a"
              href={lenkeTilOpprettAvtaleForEnv()}
              target="_blank"
              variant="primary"
              className={styles.deleknapp}
              aria-label="Opprett avtale"
              data-testid="opprettavtaleknapp"
            >
              Opprett avtale
            </Button>
          )}
          <Button
            onClick={handleClickApneModal}
            variant="secondary"
            className={styles.deleknapp}
            aria-label="Dele"
            data-testid="deleknapp"
            iconPosition="left"
          >
            Del med bruker
          </Button>
        </div>
        <TiltaksdetaljerFane />
        <Delemodal
          modalOpen={delemodalApen}
          setModalOpen={() => setDelemodalApen(false)}
          tiltaksgjennomforingsnavn={tiltaksgjennomforing.tiltaksgjennomforingNavn}
          brukerNavn={'{Navn}'}
          chattekst={tiltaksgjennomforing.tiltakstype.delingMedBruker ?? ''}
          veiledernavn={'{Veiledernavn}'}
        />
      </div>
    </>
  );
}
