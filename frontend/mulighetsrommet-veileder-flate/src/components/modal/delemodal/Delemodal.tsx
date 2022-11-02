import { Alert, Modal } from '@navikt/ds-react';
import classNames from 'classnames';
import { useReducer } from 'react';
import { logEvent } from '../../../core/api/logger';
import { capitalize, erPreview } from '../../../utils/Utils';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';
import { DelMedBrukerContent } from './DelMedBrukerContent';
import { DelMedBrukerFeiletContent } from './DelMedBrukerFeiletContent';
import { Infomelding } from './Infomelding';
import { SendtOkContent } from './SendtOkContent';

export const logDelMedbrukerEvent = (
  action:
    | 'Åpnet dialog'
    | 'Delte med bruker'
    | 'Del med bruker feilet'
    | 'Avbrutt del med bruker'
    | 'Redigerer tekstfelt'
) => {
  logEvent('mulighetsrommet.del-med-bruker', {
    value: action,
  });
};

interface DelemodalProps {
  modalOpen: boolean;
  setModalOpen: () => void;
  tiltaksgjennomforingsnavn: string;
  brukernavn?: string;
  chattekst: string;
  veiledernavn?: string;
}

export function reducer(state: State, action: Actions): State {
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
    case 'Redigerer tekstfelt':
      return { ...state, redigererTekstfelt: true };
    case 'Tilbakestill tekstfelt':
      return initInitialState(state.malTekst);
    default:
      return state;
  }
}

export function initInitialState(startTekst: string): State {
  return {
    tekst: startTekst,
    sendtStatus: 'IKKE_SENDT',
    dialogId: '',
    malTekst: startTekst,
    redigererTekstfelt: false,
  };
}

function sySammenBrukerTekst(
  chattekst: string,
  tiltaksgjennomforingsnavn: string,
  brukernavn?: string,
  veiledernavn?: string
) {
  return `${chattekst
    .replace(' <Fornavn>', brukernavn ? ` ${capitalize(brukernavn)}` : '')
    .replace('<tiltaksnavn>', tiltaksgjennomforingsnavn)}${veiledernavn ? `\n\nHilsen ${veiledernavn}` : ''}`;
}

const Delemodal = ({
  modalOpen,
  setModalOpen,
  tiltaksgjennomforingsnavn,
  brukernavn,
  chattekst,
  veiledernavn,
}: DelemodalProps) => {
  const startTekst = sySammenBrukerTekst(chattekst, tiltaksgjennomforingsnavn, brukernavn, veiledernavn);
  const [state, dispatch] = useReducer(reducer, startTekst, initInitialState);

  const clickCancel = (log = true) => {
    setModalOpen();
    dispatch({ type: 'Avbryt' });
    log && logDelMedbrukerEvent('Avbrutt del med bruker');
  };

  return (
    <Modal
      shouldCloseOnOverlayClick={false}
      closeButton={false}
      open={modalOpen}
      onClose={clickCancel}
      className={classNames(modalStyles.overstyrte_styles_fra_ds_modal, delemodalStyles.delemodal)}
      aria-label="modal"
      data-testid="delemodal"
    >
      <Modal.Content>
        {!['SENDT_OK', 'SENDING_FEILET'].includes(state.sendtStatus) && (
          <>
            <DelMedBrukerContent
              tiltaksgjennomforingsnavn={tiltaksgjennomforingsnavn}
              startTekst={startTekst}
              onCancel={clickCancel}
              state={state}
              dispatch={dispatch}
              veiledernavn={veiledernavn}
              brukernavn={brukernavn}
            />
            <Infomelding>
              Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen
            </Infomelding>
            {erPreview && (
              <Alert variant="warning">
                Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn og veileders navn blir
                automatisk satt utenfor forhåndsvisningsmodus.
              </Alert>
            )}
          </>
        )}
        {state.sendtStatus !== 'SENDT_OK' && <SendtOkContent state={state} onCancel={clickCancel} />}
        {state.sendtStatus !== 'SENDING_FEILET' && (
          <DelMedBrukerFeiletContent dispatch={dispatch} onCancel={clickCancel} />
        )}
      </Modal.Content>
    </Modal>
  );
};
export default Delemodal;
