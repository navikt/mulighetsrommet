import { ClockDashedIcon } from '@navikt/aksel-icons';
import { Button } from '@navikt/ds-react';
import StandardModal from '../modal/StandardModal';
import btnStyles from './HistorikkButton.module.scss';
import { HistorikkForBrukerModal } from './HistorikkForBrukerModal';
import styles from './HistorikkForBrukerModal.module.scss';
import { logEvent } from '../../core/api/logger';

interface Props {
  setHistorikkModalOpen: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function HistorikkButton({ setHistorikkModalOpen, isHistorikkModalOpen }: Props) {
  const handleClick = () => {
    setHistorikkModalOpen(true);
    logEvent('mulighetsrommet.historikk');
  };

  return (
    <>
      <Button variant="tertiary" onClick={handleClick} id="historikk_knapp" className={btnStyles.historikk_knapp}>
        <ClockDashedIcon aria-label="Historikk" fontSize="1.7rem" />
        Historikk
      </Button>
      <StandardModal
        className={styles.historikk_modal}
        hideButtons
        modalOpen={isHistorikkModalOpen}
        setModalOpen={() => setHistorikkModalOpen(false)}
        heading="Historikk"
        id="historikk_modal"
      >
        <HistorikkForBrukerModal />
      </StandardModal>
    </>
  );
}
