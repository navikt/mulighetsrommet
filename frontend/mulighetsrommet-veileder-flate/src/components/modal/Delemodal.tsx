import { Detail, Button, Heading, Modal, Textarea } from '@navikt/ds-react';
import React, { useReducer, useState } from 'react';
import './delemodal.less';
import { BodyLong } from '@navikt/ds-react';
import classNames from 'classnames';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';
import { APPLICATION_NAME } from '../../constants';
import { useNavigate } from 'react-router-dom';

interface DelemodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksgjennomforingsnavn: string;
  brukerNavn: string;
  chattekst: string;
}

interface State {
  tekst: string;
  sendtStatus: Status;
  dialogId: string;
  malTekst: string;
}

type Status = 'IKKE_SENDT' | 'SENDER' | 'SENDT_OK' | 'SENDING_FEILET';

interface SEND_MELDING_ACTION {
  type: 'Send melding';
}

interface AVBRYT_ACTION {
  type: 'Avbryt';
}

interface SET_TEKST_ACTION {
  type: 'Sett tekst';
  payload: string;
}

interface SENDT_OK_ACTION {
  type: 'Sendt ok';
  payload: string;
}

interface SENDING_FEILET_ACTION {
  type: 'Sending feilet';
}

interface RESET_ACTION {
  type: 'Reset';
}

type Actions =
  | SEND_MELDING_ACTION
  | SET_TEKST_ACTION
  | AVBRYT_ACTION
  | SENDT_OK_ACTION
  | SENDING_FEILET_ACTION
  | RESET_ACTION;

function reducer(state: State, action: Actions): State {
  switch (action.type) {
    case 'Avbryt':
      return { ...state, sendtStatus: 'IKKE_SENDT', tekst: state.malTekst };
    case 'Send melding':
      return { ...state, sendtStatus: 'SENDER' };
    case 'Sendt ok':
      return { ...state, sendtStatus: 'SENDT_OK', tekst: state.malTekst, dialogId: action.payload };
    case 'Sending feilet':
      return { ...state, sendtStatus: 'SENDING_FEILET' };
    case 'Sett tekst':
      return { ...state, tekst: action.payload, sendtStatus: 'IKKE_SENDT' };
    case 'Reset':
      return initInitialState(state.tekst);
    default:
      return state;
  }
}

function initInitialState(startTekst: string): State {
  return {
    tekst: startTekst,
    sendtStatus: 'IKKE_SENDT',
    dialogId: '',
    malTekst: startTekst,
  };
}

const Delemodal = ({ modalOpen, setModalOpen, tiltaksgjennomforingsnavn, brukerNavn, chattekst }: DelemodalProps) => {
  const startText = chattekst.replace('<Fornavn>', brukerNavn).replace('<tiltaksnavn>', tiltaksgjennomforingsnavn);
  const [state, dispatch] = useReducer(reducer, startText, initInitialState);
  const fnr = useHentFnrFraUrl();
  const navigate = useNavigate();

  const handleSend = async () => {
    dispatch({ type: 'Send melding' });
    const overskrift = `Tiltak gjennom NAV: ${tiltaksgjennomforingsnavn}`;
    const { tekst } = state;
    if (fnr) {
      const res = await fetch(`/veilarbdialog/api/dialog?fnr=${fnr}`, {
        method: 'POST',
        headers: {
          'Nav-Consumer-Id': APPLICATION_NAME,
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ overskrift, tekst }),
      });

      if (res.ok) {
        const { id } = (await res.json()) as { id: string };
        dispatch({ type: 'Sendt ok', payload: id });
      } else {
        dispatch({ type: 'Sending feilet' });
      }
    }
  };

  const clickCancel = () => {
    setModalOpen();
    dispatch({ type: 'Avbryt' });
  };

  const gaTilDialogen = () => {
    const origin = window.location.origin;
    window.location.href = `${origin}/${fnr}/${state.dialogId}#visDialog`;
  };

  const senderTilDialogen = state.sendtStatus === 'SENDER';

  return (
    <Modal
      shouldCloseOnOverlayClick={false}
      closeButton
      open={modalOpen}
      onClose={clickCancel}
      className={classNames('mulighetsrommet-veileder-flate__modal', 'delemodal')}
      aria-label="modal"
    >
      {state.sendtStatus !== 'SENDT_OK' && state.sendtStatus !== 'SENDING_FEILET' && (
        <Modal.Content>
          <Heading spacing level="1" size="large" data-testid="modal_header">
            {'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
          </Heading>
          <BodyLong>
            Kandidatene blir varslet på SMS/e-post, og kan se informasjon om tiltaket i aktivitetsplanen på Ditt NAV.
          </BodyLong>
          <Textarea
            value={state.tekst}
            onChange={e => dispatch({ type: 'Sett tekst', payload: e.currentTarget.value })}
            label=""
            minRows={5}
            data-testid="textarea_tilbakemelding"
          />
          <div className="modal_btngroup">
            <Button onClick={handleSend} data-testid="modal_btn-send" disabled={senderTilDialogen}>
              {senderTilDialogen ? 'Sender...' : 'Send via Dialogen'}
            </Button>
            <Button
              variant="tertiary"
              onClick={clickCancel}
              data-testid="modal_btn-cancel"
              disabled={senderTilDialogen}
            >
              Avbryt
            </Button>
          </div>
        </Modal.Content>
      )}
      {state.sendtStatus === 'SENDT_OK' && (
        <Modal.Content>
          <Heading spacing level="1" size="large" data-testid="modal_header">
            Meldingen er sendt
          </Heading>
          <Detail>Du kan fortsette dialogen om dette tiltaket i Dialogen.</Detail>
          <div className="modal_btngroup">
            <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Lukk
            </Button>
            <Button variant="tertiary" onClick={gaTilDialogen} data-testid="modal_btn-cancel">
              Gå til Dialogen
            </Button>
          </div>
        </Modal.Content>
      )}
      {state.sendtStatus === 'SENDING_FEILET' && (
        <Modal.Content>
          <Heading spacing level="1" size="large" data-testid="modal_header">
            Kunne ikke sende melding via Dialogen
          </Heading>
          <p>
            Vi klarte ikke dele informasjon med bruker. Det kan være fordi brukeren er under manuell oppfølging, har
            reservert seg i <abbr title="Kontakt- og reservasjonsregisteret">KRR</abbr> eller ikke har vært logget inn
            på NAV.no de siste 18 månedene.
          </p>
          <div className="modal_btngroup">
            <Button variant="tertiary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Lukk
            </Button>
            <Button variant="tertiary" onClick={() => dispatch({ type: 'Reset' })} data-testid="modal_btn-cancel">
              Prøv på nytt
            </Button>
          </div>
        </Modal.Content>
      )}
    </Modal>
  );
};
export default Delemodal;
