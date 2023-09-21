import { useAtom } from 'jotai';
import { useEffect, useState } from 'react';
import Filtermeny from '../../components/filtrering/Filtermeny';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { FiltrertFeilInnsatsgruppeVarsel } from '../../components/ikkeKvalifisertVarsel/FiltrertFeilInnsatsgruppeVarsel';
import { OversiktenJoyride } from '../../components/joyride/OversiktenJoyride';
import { OversiktenLastStepJoyride } from '../../components/joyride/OversiktenLastStepJoyride';
import Tiltaksgjennomforingsoversikt from '../../components/oversikt/Tiltaksgjennomforingsoversikt';
import useTiltaksgjennomforinger from '../../core/api/queries/useTiltaksgjennomforinger';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import styles from './ViewTiltaksgjennomforingOversikt.module.scss';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import Tilbakeknapp from '../../components/tilbakeknapp/Tilbakeknapp';
import { useFeatureToggle } from '../../core/api/feature-toggles';
import { Toggles } from 'mulighetsrommet-api-client';

const ViewTiltaksgjennomforingOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const [isHistorikkModalOpen, setIsHistorikkModalOpen] = useState(false);
  const { isFetched } = useTiltaksgjennomforinger();
  const brukerdata = useHentBrukerdata();
  const landingssideFeature = useFeatureToggle(Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE);
  const landingssideEnabled = landingssideFeature.isSuccess && landingssideFeature.data;

  useEffect(() => {
    setIsHistorikkModalOpen(isHistorikkModalOpen);
  }, [isHistorikkModalOpen]);

  if (!brukerdata.data) return null;

  return (
    <>
      {landingssideEnabled ? <Tilbakeknapp tilbakelenke="/" /> : null}
      <div className={styles.tiltakstype_oversikt} data-testid="tiltakstype-oversikt">
        <Filtermeny />
        <div className={styles.filtertags_og_knapperad}>
          <Filtertags filter={filter} setFilter={setFilter} />
          <div className={styles.knapperad}>
            <>
              <OversiktenJoyride isTableFetched={isFetched} />
              <OversiktenLastStepJoyride />
            </>
            <HistorikkButton
              setHistorikkModalOpen={setIsHistorikkModalOpen}
              isHistorikkModalOpen={isHistorikkModalOpen}
            />
          </div>
        </div>
        <div>
          <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
          <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata.data} />
          <Tiltaksgjennomforingsoversikt />
        </div>
      </div>
    </>
  );
};

export default ViewTiltaksgjennomforingOversikt;
