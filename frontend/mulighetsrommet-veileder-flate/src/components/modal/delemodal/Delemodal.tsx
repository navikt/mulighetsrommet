import { Modal } from '@navikt/ds-react';
import classNames from 'classnames';
import { useReducer } from 'react';
import { logEvent } from '../../../core/api/logger';
import { capitalize } from '../../../utils/Utils';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';
import { DelMedBrukerContent } from './DelMedBrukerContent';
import { DelMedBrukerFeiletContent } from './DelMedBrukerFeiletContent';
import { Infomelding } from './Infomelding';
import { SendtOkContent } from './SendtOkContent';

export const logDelMedbrukerEvent = (
  action: 'Åpnet dialog' | 'Delte med bruker' | 'Del med bruker feilet' | 'Avbrutt del med bruker' | 'Redigerer hilsen'
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
      return { ...state, sendtStatus: 'IKKE_SENDT', hilsen: state.originalHilsen };
    case 'Send melding':
      return { ...state, sendtStatus: 'SENDER' };
    case 'Sendt ok':
      return { ...state, sendtStatus: 'SENDT_OK', dialogId: action.payload };
    case 'Sending feilet':
      return { ...state, sendtStatus: 'SENDING_FEILET' };
    case 'Sett hilsen':
      return { ...state, hilsen: action.payload, sendtStatus: 'IKKE_SENDT' };
    case 'Reset':
      return initInitialState({ originalHilsen: state.originalHilsen, deletekst: state.deletekst });
    default:
      return state;
  }
}

export function initInitialState(tekster: { deletekst: string; originalHilsen: string }): State {
  return {
    deletekst: tekster.deletekst,
    originalHilsen: tekster.originalHilsen,
    hilsen: tekster.originalHilsen,
    sendtStatus: 'IKKE_SENDT',
    dialogId: '',
  };
}

function sySammenBrukerTekst(chattekst: string, tiltaksgjennomforingsnavn: string, brukernavn?: string) {
  return `${chattekst
    .replace(' <Fornavn>', brukernavn ? ` ${capitalize(brukernavn)}` : '')
    .replace('<tiltaksnavn>', tiltaksgjennomforingsnavn)}`;
}

function sySammenHilsenTekst(veiledernavn?: string) {
  const interessant =
    'Er dette tiltaket aktuelt for deg? Gi meg gjerne et ja/nei svar her i dialogen.\nDitt svar (ja/nei) vil ikke påvirke ditt forhold til NAV.';
  return veiledernavn ? `${interessant}\n\nHilsen ${veiledernavn}` : `${interessant}\n\nHilsen `;
}

const Delemodal = ({
  modalOpen,
  setModalOpen,
  tiltaksgjennomforingsnavn,
  brukernavn,
  chattekst,
  veiledernavn,
}: DelemodalProps) => {
  const deletekst = sySammenBrukerTekst(chattekst, tiltaksgjennomforingsnavn, brukernavn);
  const originalHilsen = sySammenHilsenTekst(veiledernavn);
  const [state, dispatch] = useReducer(reducer, { deletekst, originalHilsen }, initInitialState);

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
              onCancel={clickCancel}
              state={state}
              dispatch={dispatch}
              veiledernavn={veiledernavn}
              brukernavn={brukernavn}
            />
            <Infomelding>
              Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen
            </Infomelding>
          </>
        )}
        {state.sendtStatus === 'SENDT_OK' && <SendtOkContent state={state} onCancel={clickCancel} />}
        {state.sendtStatus === 'SENDING_FEILET' && (
          <DelMedBrukerFeiletContent dispatch={dispatch} onCancel={clickCancel} />
        )}
      </Modal.Content>
    </Modal>
  );
};
export default Delemodal;
