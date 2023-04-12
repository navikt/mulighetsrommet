import { useAtom } from 'jotai';
import Filtermeny from '../../components/filtrering/Filtermeny';
import { Filtertags } from '../../components/filtrering/Filtertags';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { BrukerHarIkke14aVedtakVarsel } from '../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel';
import { FiltrertFeilInnsatsgruppeVarsel } from '../../components/ikkeKvalifisertVarsel/FiltrertFeilInnsatsgruppeVarsel';
import Tiltaksgjennomforingsoversikt from '../../components/oversikt/Tiltaksgjennomforingsoversikt';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import styles from './ViewTiltaksgjennomforingOversikt.module.scss';
import { useModal } from '../../hooks/useModal';
import { OversiktenJoyride } from '../../components/joyride/OversiktenJoyride';

const ViewTiltaksgjennomforingOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { isOpen: isHistorikkModalOpen, toggle: toggleHistorikkmodal } = useModal();

  return (
    <div className={styles.tiltakstype_oversikt} id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <Filtermeny />
      <div className={styles.filtertags_og_knapperad}>
        <Filtertags filter={filter} setFilter={setFilter} />
        <div className={styles.knapperad}>
          <OversiktenJoyride toggleHistorikkModal={toggleHistorikkmodal} />
          <HistorikkButton toggleHistorikkModal={toggleHistorikkmodal} isOpen={isHistorikkModalOpen} />
        </div>
      </div>
      <div>
        <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
        <BrukerHarIkke14aVedtakVarsel />
        <Tiltaksgjennomforingsoversikt />
      </div>
    </div>
  );
};

export default ViewTiltaksgjennomforingOversikt;
