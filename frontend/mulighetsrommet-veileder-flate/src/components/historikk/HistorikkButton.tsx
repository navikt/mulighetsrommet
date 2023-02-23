import { Button } from '@navikt/ds-react';
import { useState } from 'react';
import StandardModal from '../modal/StandardModal';
import { HistorikkForBrukerModal } from './HistorikkForBrukerModal';
import styles from './HistorikkForBrukerModal.module.scss';
import btnStyles from './HistorikkButton.module.scss';
import { Historic } from '@navikt/ds-icons';
import { logEvent } from '../../core/api/logger';

export function HistorikkButton() {
  const [apneModal, setApneModal] = useState(false);
  const toggleModal = () => setApneModal(!apneModal);

  const handleClick = () => {
    toggleModal();
    logEvent('mulighetsrommet.historikk');
  };

  return (
    <>
      <Button onClick={handleClick} variant="tertiary" className={btnStyles.historikk_knapp}>
        <Historic aria-label="Historikk" />
      </Button>
      <StandardModal
        className={styles.historikk_modal}
        hideButtons
        modalOpen={apneModal}
        setModalOpen={toggleModal}
        heading="Historikk"
      >
        <HistorikkForBrukerModal />
      </StandardModal>
    </>
  );
}
