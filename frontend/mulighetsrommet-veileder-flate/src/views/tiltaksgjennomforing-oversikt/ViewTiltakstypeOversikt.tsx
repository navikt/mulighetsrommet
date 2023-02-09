import { useAtom } from 'jotai';
import Filtermeny from '../../components/filtrering/Filtermeny';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { FiltrertFeilInnsatsgruppeVarsel } from '../../components/ikkeKvalifisertVarsel/FiltrertFeilInnsatsgruppeVarsel';
import Tiltaksgjennomforingsoversikt from '../../components/oversikt/Tiltaksgjennomforingsoversikt';
import { useFeatureToggles, VIS_HISTORIKK } from '../../core/api/feature-toggles';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import styles from './ViewTiltakstypeOversikt.module.scss';

const ViewTiltakstypeOversikt = () => {
  const features = useFeatureToggles();
  const visHistorikkKnapp = features.isSuccess && features.data[VIS_HISTORIKK];
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  return (
    <div className={styles.tiltakstype_oversikt} id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <Filtermeny />
      <div className={styles.filtertags_og_historikk}>
        <Filtertags filter={filter} setFilter={setFilter} />
        {visHistorikkKnapp && <HistorikkButton />}
      </div>
      <div>
        <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
        <BrukerHarIkke14aVedtakVarsel />
        <Tiltaksgjennomforingsoversikt />
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
