import Filtermeny from '../../components/filtrering/Filtermeny';
import Tiltaksgjennomforingsoversikt from '../../components/oversikt/Tiltaksgjennomforingsoversikt';
import styles from './ViewTiltakstypeOversikt.module.scss';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { useFeatureToggles, VIS_HISTORIKK } from '../../core/api/feature-toggles';

const ViewTiltakstypeOversikt = () => {
  const features = useFeatureToggles();
  const visHistorikkKnapp = features.isSuccess && features.data[VIS_HISTORIKK];

  return (
    <div className={styles.tiltakstype_oversikt} id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <div className={styles.filtertags_og_historikk}>
        <Filtertags />
        {visHistorikkKnapp && <HistorikkButton />}
      </div>
      <Filtermeny />
      <div>
        <Tiltaksgjennomforingsoversikt />
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
