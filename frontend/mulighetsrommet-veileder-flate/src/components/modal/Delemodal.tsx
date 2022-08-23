import HelpText, { Button, Heading, Modal, Textarea } from '@navikt/ds-react';
import React, { useState } from 'react';
import './modal.less';
import StandardModal from './StandardModal';
import { BodyLong } from '@navikt/ds-react';
import classNames from 'classnames';
import ModalContent from '@navikt/ds-react/esm/modal/ModalContent';

interface DelemodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksgjennomforingsnavn: string;
  brukerNavn: string;
  chattekst: string;
}

const Delemodal = ({ modalOpen, setModalOpen, tiltaksgjennomforingsnavn, brukerNavn, chattekst }: DelemodalProps) => {
  const startText = 'Hei, ' + brukerNavn + '!\n' + chattekst;
  const [verdi, setVerdi] = useState(startText);
  const [meldingSendt, setMeldingSendt] = useState(false);

  const handleSend = () => {
    setVerdi(startText);
    setMeldingSendt(true);
    //toast.info('Takk for din tilbakemelding!');
  };
  const clickSend = () => {
    //setModalOpen();
    handleSend();
  };

  const clickCancel = () => {
    setModalOpen();
    setVerdi(startText);
  };

  const gaTilDialogen = () => {
  };

  return (
    <Modal
      shouldCloseOnOverlayClick={false}
      closeButton
      open={modalOpen}
      onClose={setModalOpen}
      className={classNames('mulighetsrommet-veileder-flate__modal', 'delemodal')}
      aria-label="modal"
    >
      {!meldingSendt && (
        <Modal.Content>
          <Heading spacing level="1" size="large" data-testid="modal_header">
            {'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
          </Heading>
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
          <div className="modal_btngroup">
            <Button onClick={clickSend} data-testid="modal_btn-send">
              Send via dialogen
            </Button>
            <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Avbryt
            </Button>
          </div>
        </Modal.Content>
      )}
      {meldingSendt && (
        <Modal.Content>
          <Heading spacing level="1" size="large" data-testid="modal_header">
            Meldingen er sendt
          </Heading>
          <BodyLong>
            Du kan fortsette dialogen om dette tiltaket i dialogen.
          </BodyLong>
          <div className="modal_btngroup">
            <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Lukk
            </Button>
            <Button variant="tertiary" onClick={gaTilDialogen} data-testid="modal_btn-cancel">
              Gå til dialogen
            </Button>
          </div>
        </Modal.Content>
      )}
    </Modal>
  );
  /*
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
    */
};
export default Delemodal;
