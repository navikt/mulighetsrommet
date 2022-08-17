import { Textarea } from '@navikt/ds-react';
import React, { useState } from 'react';
import './modal.less';
import StandardModal from './StandardModal';
import { logEvent } from '../../core/api/logger';
import { toast } from 'react-toastify';

interface TilbakemeldingsmodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
}

const Tilbakemeldingsmodal = ({ modalOpen, setModalOpen }: TilbakemeldingsmodalProps) => {
  const [verdi, setVerdi] = useState('');

  const handleSend = () => {
    logEvent('mulighetsrommet.tilbakemelding.detaljside', { kommentar: verdi });
    setVerdi('');
    toast.info('Takk for din tilbakemelding!');
  };

  return (
    <StandardModal
      className="tilbakemeldingsmodal"
      modalOpen={modalOpen}
      setModalOpen={setModalOpen}
      heading="Tilbakemelding"
      handleForm={handleSend}
      handleCancel={() => setVerdi('')}
      shouldCloseOnOverlayClick={false}
    >
      <Textarea
        value={verdi}
        onChange={e => setVerdi(e.target.value)}
        label="Hva er din tilbakemelding?"
        minRows={5}
        data-testid="textarea_tilbakemelding"
      />
    </StandardModal>
  );
};

export default Tilbakemeldingsmodal;
