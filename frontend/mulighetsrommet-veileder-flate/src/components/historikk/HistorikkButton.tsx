import { Button } from '@navikt/ds-react';
import { useState } from 'react';
import StandardModal from '../modal/StandardModal';
import { HistorikkForBruker } from './HistorikkForBruker';
import './HistorikkForBruker.less';

export function HistorikkButton() {
  const [apneModal, setApneModal] = useState(false);
  const toggleModal = () => setApneModal(!apneModal);

  return (
    <>
      <Button onClick={toggleModal}>Historikk</Button>
      <StandardModal
        className="historikk-modal"
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
