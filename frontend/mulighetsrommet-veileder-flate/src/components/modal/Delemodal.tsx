import HelpText, { Textarea } from '@navikt/ds-react';
import React, { useState } from 'react';
import './modal.less';
import StandardModal from './StandardModal';
import { logEvent } from '../../core/api/logger';
import { toast } from 'react-toastify';
import { BodyLong } from '@navikt/ds-react';

interface DelemodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksgjennomforingsnavn: string;
  brukerNavn: string;
  chattekst: string;
}

const Delemodal = ({ modalOpen, setModalOpen, tiltaksgjennomforingsnavn, brukerNavn, chattekst }: DelemodalProps) => {
  const startText = "Hei, " + brukerNavn + "!\n" + chattekst;
  const [verdi, setVerdi] = useState(startText);

  const handleSend = () => {
    setVerdi(startText);
    toast.info('Takk for din tilbakemelding!');
  };

  return (
    <StandardModal
      className="delemodal"
      modalOpen={modalOpen}
      setModalOpen={() => {
        setModalOpen();
        setVerdi(startText);
      }}
      heading={'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
      handleForm={handleSend}
      handleCancel={() => setVerdi(startText)}
      shouldCloseOnOverlayClick={false}
      btnText="Send via dialogen"
    >
      <BodyLong>
        Kandidatene blir varslet på SMS/e-post, og kan se informasjon om tiltaket på i aktivitetsplanen på Ditt NAV.
      </BodyLong>
      <Textarea
        value={verdi}
        onChange={e => setVerdi(e.target.value)}
        label=""
        minRows={5}
        data-testid="textarea_tilbakemelding"
      />
    </StandardModal>
  );
};

export default Delemodal;
