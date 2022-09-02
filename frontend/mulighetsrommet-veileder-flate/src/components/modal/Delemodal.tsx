import { BodyLong, Button, Detail, Heading, Modal, Textarea } from '@navikt/ds-react';
import classNames from 'classnames';
import { useReducer } from 'react';
import { APPLICATION_NAME } from '../../constants';
import { logEvent } from '../../core/api/logger';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';
import './delemodal.less';
import { mulighetsrommetClient } from '../../core/api/clients';

export const logDelMedbrukerEvent = (
  action: 'Åpnet dialog' | 'Delte med bruker' | 'Del med bruker feilet' | 'Avbrutt del med bruker'
) => {
  logEvent('mulighetsrommet.del-med-bruker', {
    value: action,
  });
};

interface DelemodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksgjennomforingsnavn: string;
  brukerNavn: string;
  chattekst: string;
  veiledernavn?: string;
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

const Delemodal = ({
  modalOpen,
  setModalOpen,
  tiltaksgjennomforingsnavn,
  brukerNavn,
  chattekst,
  veiledernavn = '',
}: DelemodalProps) => {
  const startText = `${chattekst
    .replace('<Fornavn>', brukerNavn)
    .replace('<tiltaksnavn>', tiltaksgjennomforingsnavn)}\n\nHilsen ${veiledernavn}`;
  const [state, dispatch] = useReducer(reducer, startText, initInitialState);
  const fnr = useHentFnrFraUrl();

  const getAntallTegn = () => {
    return startText.length + 200;
  };

  const handleSend = async () => {
    if (state.tekst.trim().length > getAntallTegn()) return;
    logDelMedbrukerEvent('Delte med bruker');

    dispatch({ type: 'Send melding' });
    const overskrift = `Tiltak gjennom NAV: ${tiltaksgjennomforingsnavn}`;
    const { tekst } = state;
    if (fnr) {
      const res = await mulighetsrommetClient.dialogen.delMedDialogen({ fnr, requestBody: { overskrift, tekst } });

      if (res) {
        dispatch({ type: 'Sendt ok', payload: res.id });
      } else {
        dispatch({ type: 'Sending feilet' });
        logDelMedbrukerEvent('Del med bruker feilet');
      }
    }
  };

  const clickCancel = () => {
    setModalOpen();
    dispatch({ type: 'Avbryt' });
    logDelMedbrukerEvent('Avbrutt del med bruker');
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
            Bruker blir varslet på SMS/e-post, og kan se informasjon om tiltaket i aktivitetsplanen på Min side.
          </BodyLong>
          <Textarea
            value={state.tekst}
            onChange={e => dispatch({ type: 'Sett tekst', payload: e.currentTarget.value })}
            label=""
            minRows={10}
            maxRows={50}
            data-testid="textarea_tilbakemelding"
            maxLength={getAntallTegn()}
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
        <Modal.Content className="delemodal__content">
          <Heading spacing level="1" size="large" data-testid="modal_header">
            Tiltaket kunne ikke deles med brukeren
          </Heading>
          <p className="delemodal__content">
            Vi kunne ikke dele informasjon digitalt med denne brukeren. Dette kan være fordi hen ikke ønsker eller kan
            benytte de digitale tjenestene våre.
            <br />
            <a href="https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-arbeidsrettet-brukeroppfolging/SitePages/Manuell-oppf%C3%B8lging-i-Modia-arbeidsrettet-oppf%C3%B8lging.aspx">
              Les mer om manuell oppfølging
            </a>
          </p>
          <div className="modal_btngroup">
            <Button variant="primary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Avslutt
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
