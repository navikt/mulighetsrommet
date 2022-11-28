import Filtermeny from '../../components/filtrering/Filtermeny';
import Tiltaksgjennomforingsoversikt from '../../components/oversikt/Tiltaksgjennomforingsoversikt';
import styles from './ViewTiltakstypeOversikt.module.scss';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { useFeatureToggles, VIS_HISTORIKK } from '../../core/api/feature-toggles';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { FiltrertFeilInnsatsgruppeVarsel } from '../../components/ikkeKvalifisertVarsel/FiltrertFeilInnsatsgruppeVarsel';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';

const ViewTiltakstypeOversikt = () => {
  const features = useFeatureToggles();
  const visHistorikkKnapp = features.isSuccess && features.data[VIS_HISTORIKK];
  const brukerData = useHentBrukerdata();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  // console.log(brukerData);
  return (
    <div className={styles.tiltakstype_oversikt} id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <div className={styles.filtertags_og_historikk}>
        <Filtertags filter={filter} setFilter={setFilter} />
        {visHistorikkKnapp && <HistorikkButton />}
      </div>
      <Filtermeny />
      <div>
        <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
        <BrukerHarIkke14aVedtakVarsel />
        <Tiltaksgjennomforingsoversikt />
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
