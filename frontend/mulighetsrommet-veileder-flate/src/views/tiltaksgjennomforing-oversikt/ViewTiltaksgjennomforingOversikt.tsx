import { useAtom } from 'jotai';
import Filtermeny from '../../components/filtrering/Filtermeny';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { FiltrertFeilInnsatsgruppeVarsel } from '../../components/ikkeKvalifisertVarsel/FiltrertFeilInnsatsgruppeVarsel';
import Tiltaksgjennomforingsoversikt from '../../components/oversikt/Tiltaksgjennomforingsoversikt';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import styles from './ViewTiltaksgjennomforingOversikt.module.scss';
import { OversiktenJoyride } from '../../components/joyride/OversiktenJoyride';
import { useFeatureToggles, VIS_JOYRIDE } from '../../core/api/feature-toggles';
import { useEffect, useState } from 'react';
import { OversiktenLastStepJoyride } from '../../components/joyride/OversiktenLastStepJoyride';
import useTiltaksgjennomforinger from '../../core/api/queries/useTiltaksgjennomforinger';

const ViewTiltaksgjennomforingOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const [isHistorikkModalOpen, setIsHistorikkModalOpen] = useState(false);
  const features = useFeatureToggles();
  const visJoyride = features.isSuccess && features.data[VIS_JOYRIDE];
  const { data } = useTiltaksgjennomforinger();

  useEffect(() => {
    setIsHistorikkModalOpen(isHistorikkModalOpen);
  }, [isHistorikkModalOpen]);

  return (
    <>
      <div className={styles.tiltakstype_oversikt} data-testid="tiltakstype-oversikt">
        <Filtermeny />
        <div className={styles.filtertags_og_knapperad}>
          <Filtertags filter={filter} setFilter={setFilter} />
          <div className={styles.knapperad}>
            {visJoyride && (
              <>
                <OversiktenJoyride
                  setHistorikkModalOpen={setIsHistorikkModalOpen}
                  isHistorikkModalOpen={isHistorikkModalOpen}
                  isTableFetched={!!data}
                />
                <OversiktenLastStepJoyride />
              </>
            )}
            <HistorikkButton
              setHistorikkModalOpen={setIsHistorikkModalOpen}
              isHistorikkModalOpen={isHistorikkModalOpen}
            />
          </div>
        </div>
        <div>
          <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
          <BrukerHarIkke14aVedtakVarsel />
          <Tiltaksgjennomforingsoversikt />
        </div>
      </div>
    </>
  );
};

export default ViewTiltaksgjennomforingOversikt;
