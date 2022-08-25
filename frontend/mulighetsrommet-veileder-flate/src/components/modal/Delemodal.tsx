import { Button, Heading, Modal, Textarea } from '@navikt/ds-react';
import React, { useState } from 'react';
import './delemodal.less';
import { BodyLong } from '@navikt/ds-react';
import classNames from 'classnames';

interface DelemodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksgjennomforingsnavn?: string;
  brukerNavn?: string;
  chattekst: string;
}

const Delemodal = ({ modalOpen, setModalOpen, tiltaksgjennomforingsnavn, brukerNavn, chattekst }: DelemodalProps) => {
  const startText = chattekst
    .replace('<Fornavn>', brukerNavn ?? '')
    .replace('<tiltaksnavn>', tiltaksgjennomforingsnavn ?? '');
  const [verdi, setVerdi] = useState(startText);
  const [meldingSendt, setMeldingSendt] = useState(false);

  const handleSend = () => {
    setVerdi(startText);
    setMeldingSendt(true);
  };

  const clickCancel = () => {
    setModalOpen();
    setVerdi(startText);
    setMeldingSendt(false);
  };

  const gaTilDialogen = () => {};

  return (
    <Modal
      shouldCloseOnOverlayClick={false}
      closeButton
      open={modalOpen}
      onClose={clickCancel}
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
            <Button onClick={handleSend} data-testid="modal_btn-send">
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
          <BodyLong>Du kan fortsette dialogen om dette tiltaket i dialogen.</BodyLong>
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
};
export default Delemodal;
