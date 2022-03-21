import { Textarea } from '@navikt/ds-react';
import React, { useState } from 'react';
import './modal.less';
import StandardModal from './StandardModal';

interface TilbakemeldingsmodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
}

const Tilbakemeldingsmodal = ({ modalOpen, setModalOpen }: TilbakemeldingsmodalProps) => {
  const [verdi, setVerdi] = useState('');

  return (
    <StandardModal
      className="tilbakemeldingsmodal"
      modalOpen={modalOpen}
      setModalOpen={setModalOpen}
      heading="Tilbakemelding"
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
