import { BodyShort, Button, Heading, Ingress, Modal, Textarea } from '@navikt/ds-react';
import { useReducer } from 'react';
import { APPLICATION_NAME } from '../../../constants';
import { logEvent } from '../../../core/api/logger';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import '../Modal.less';
import './Delemodal.less';
import { Actions, State } from './ModalActions';
import Lenke from '../../lenke/Lenke';
import { ErrorColored, SuccessColored } from '@navikt/ds-icons';

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
    if (startText.length === 0) {
      return 750;
    }
    return startText.length + 200;
  };

  const handleError = () => {
    if (state.tekst.length === 0) return 'Kan ikke sende tom melding.';
  };

  const handleSend = async () => {
    handleError();
    if (state.tekst.trim().length > getAntallTegn()) return;
    logDelMedbrukerEvent('Delte med bruker');

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
      className="mulighetsrommet-veileder-flate__modal delemodal"
      aria-label="modal"
      data-testid="delemodal"
    >
      {state.sendtStatus !== 'SENDT_OK' && state.sendtStatus !== 'SENDING_FEILET' && (
        <Modal.Content>
          <Heading size="large" level="1" data-testid="modal_header">
            Del tiltak med bruker
          </Heading>
          <Ingress>{'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}</Ingress>
          <BodyShort size="small">
            Bruker blir varslet på SMS/e-post, og kan se informasjon om tiltaket i aktivitetsplanen på Min side.
          </BodyShort>
          <Textarea
            value={state.tekst}
            onChange={e => dispatch({ type: 'Sett tekst', payload: e.currentTarget.value })}
            label=""
            minRows={10}
            maxRows={50}
            data-testid="textarea_tilbakemelding"
            maxLength={getAntallTegn()}
            error={handleError()}
          />
          <div className="modal_btngroup">
            <Button
              onClick={handleSend}
              data-testid="modal_btn-send"
              disabled={senderTilDialogen || state.tekst.length === 0}
            >
              {senderTilDialogen ? 'Sender...' : 'Send via Dialogen'}
            </Button>
            <Button
              variant="secondary"
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
        <Modal.Content className="delemodal__tilbakemelding delemodal__success">
          <SuccessColored className="delemodal__svg" />
          <Heading level="1" size="large" data-testid="modal_header">
            Meldingen er sendt
          </Heading>
          <BodyShort>Du kan fortsette dialogen om dette tiltaket i Dialogen.</BodyShort>
          <div className="modal_btngroup">
            <Button variant="primary" onClick={gaTilDialogen} data-testid="modal_btn-dialog">
              Gå til Dialogen
            </Button>
            <Button variant="secondary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Lukk
            </Button>
          </div>
        </Modal.Content>
      )}
      {state.sendtStatus === 'SENDING_FEILET' && (
        <Modal.Content className="delemodal__tilbakemelding delemodal__success">
          <ErrorColored className="delemodal__svg" />
          <Heading level="1" size="large" data-testid="modal_header">
            Tiltaket kunne ikke deles med brukeren
          </Heading>
          <BodyShort>
            Vi kunne ikke dele informasjon digitalt med denne brukeren. Dette kan være fordi hen ikke ønsker eller kan
            benytte de digitale tjenestene våre.{' '}
            <Lenke
              isExternal
              to="https://navno.sharepoint.com/sites/fag-og-ytelser-arbeid-arbeidsrettet-brukeroppfolging/SitePages/Manuell-oppf%C3%B8lging-i-Modia-arbeidsrettet-oppf%C3%B8lging.aspx"
            >
              Les mer om manuell oppfølging{' '}
            </Lenke>
          </BodyShort>
          <div className="modal_btngroup">
            <Button variant="primary" onClick={() => dispatch({ type: 'Reset' })} data-testid="modal_btn-reset">
              Prøv på nytt
            </Button>
            <Button variant="secondary" onClick={clickCancel} data-testid="modal_btn-cancel">
              Lukk
            </Button>
          </div>
        </Modal.Content>
      )}
    </Modal>
  );
};
export default Delemodal;
