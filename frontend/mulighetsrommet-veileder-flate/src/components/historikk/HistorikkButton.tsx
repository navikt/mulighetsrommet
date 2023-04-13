import { ClockDashedIcon } from '@navikt/aksel-icons';
import { Button } from '@navikt/ds-react';
import StandardModal from '../modal/StandardModal';
import btnStyles from './HistorikkButton.module.scss';
import { HistorikkForBrukerModal } from './HistorikkForBrukerModal';
import styles from './HistorikkForBrukerModal.module.scss';
import { logEvent } from '../../core/api/logger';

interface Props {
  toggleHistorikkModal: (state: boolean) => void;
  isHistorikkModalOpen: boolean;
}

export function HistorikkButton({ toggleHistorikkModal, isHistorikkModalOpen }: Props) {
  const handleClick = () => {
    toggleHistorikkModal(true);
    logEvent('mulighetsrommet.historikk');
  };

  return (
    <>
      <Button onClick={handleClick} variant="tertiary" id="historikkBtn" className={btnStyles.historikk_knapp}>
        <ClockDashedIcon aria-label="Historikk" />
      </Button>
      <StandardModal
        className={styles.historikk_modal}
        hideButtons
        modalOpen={isHistorikkModalOpen}
        setModalOpen={() => toggleHistorikkModal(false)}
        heading="Historikk"
        id="historikk_modal"
      >
        <HistorikkForBrukerModal />
      </StandardModal>
    </>
  );
}
