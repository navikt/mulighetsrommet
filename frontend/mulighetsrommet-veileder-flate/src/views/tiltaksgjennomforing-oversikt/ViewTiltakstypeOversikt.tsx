import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import styles from './ViewTiltakstypeOversikt.module.scss';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { useFeatureToggles, VIS_HISTORIKK } from '../../core/api/feature-toggles';

const ViewTiltakstypeOversikt = () => {
  const features = useFeatureToggles();
  const visHistorikkKnapp = features.isSuccess && features.data[VIS_HISTORIKK];

  return (
    <div className={styles.tiltakstypeOversikt} id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <div className={styles.filtertagsOgHistorikk}>
        <Filtertags />
        {visHistorikkKnapp && <HistorikkButton />}
      </div>
      <Filtermeny />
      <div>
        <TiltaksgjennomforingsTabell />
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
