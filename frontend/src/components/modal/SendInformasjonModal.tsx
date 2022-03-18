import { BodyLong, Textarea } from '@navikt/ds-react';
import React, { useState } from 'react';
import './modal.less';
import StandardModal from './StandardModal';

interface SendInformasjonModalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksnavn?: string;
}

const SendInformasjonModal = ({ modalOpen, setModalOpen, tiltaksnavn }: SendInformasjonModalProps) => {
  const [verdi, setVerdi] = useState('');

  return (
    <StandardModal
      className="send-informasjon-modal"
      modalOpen={modalOpen}
      setModalOpen={setModalOpen}
      heading={`Informasjon om ${tiltaksnavn}`}
      btnText="Send informasjon"
    >
      <BodyLong>
        Kandidatene blir varslet på SMS/e-post, og kan se informasjon om tiltaket i aktivitetsplanen på Ditt NAV.{' '}
      </BodyLong>
      <Textarea
        value={verdi}
        onChange={e => setVerdi(e.target.value)}
        label="Legg til kommentar til brukeren"
        minRows={5}
        data-testid="textarea_send-informasjon"
      />
    </StandardModal>
  );
};

export default SendInformasjonModal;
