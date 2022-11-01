import { Button } from '@navikt/ds-react';
import { useState } from 'react';
import StandardModal from '../modal/StandardModal';
import { HistorikkForBruker } from './HistorikkForBruker';
import styles from './HistorikkButton.module.scss';
import { Historic } from '@navikt/ds-icons';

export function HistorikkButton() {
  const [apneModal, setApneModal] = useState(false);
  const toggleModal = () => setApneModal(!apneModal);

  return (
    <>
      <Button onClick={toggleModal} variant="tertiary" className={styles.historikk_knapp}>
        <Historic aria-label="Historikk" />
      </Button>
      <StandardModal
        className={styles.historikk_modal}
        hideButtons
        modalOpen={apneModal}
        setModalOpen={toggleModal}
        heading="Aktivitet"
        handleForm={() => {}}
      >
        <HistorikkForBruker />
      </StandardModal>
    </>
  );
}
