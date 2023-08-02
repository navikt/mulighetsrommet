import { BodyShort, Modal } from '@navikt/ds-react';
import classNames from 'classnames';
import { useReducer } from 'react';
import { logEvent } from '../../../core/api/logger';
import { capitalize } from '../../../utils/Utils';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';
import { useHentBrukerdata } from '../../../core/api/queries/useHentBrukerdata';
import { KanIkkeDeleMedBrukerModal } from './KanIkkeDeleMedBrukerModal';
import { DelMedBrukerContent } from './DelMedBrukerContent';
import { useNavigerTilDialogen } from '../../../hooks/useNavigerTilDialogen';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { StatusModal } from './StatusModal';
import { PORTEN } from 'mulighetsrommet-frontend-common/constants';

export const logDelMedbrukerEvent = (
  action: 'Åpnet dialog' | 'Delte med bruker' | 'Del med bruker feilet' | 'Avbrutt del med bruker' | 'Sett hilsen'
) => {
  logEvent('mulighetsrommet.del-med-bruker', { value: action });
};

interface DelemodalProps {
  modalOpen: boolean;
  lukkModal: () => void;
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
    'Er dette aktuelt for deg? Gi meg tilbakemelding her i dialogen.\nSvaret ditt vil ikke endre din utbetaling fra NAV.';
  return veiledernavn
    ? `${interessant}\n\nVi holder kontakten!\nHilsen ${veiledernavn}`
    : `${interessant}\n\nVi holder kontakten!\nHilsen `;
}

const Delemodal = ({
  modalOpen,
  lukkModal,
  tiltaksgjennomforingsnavn,
  brukernavn,
  chattekst,
  veiledernavn,
}: DelemodalProps) => {
  const deletekst = sySammenBrukerTekst(chattekst, tiltaksgjennomforingsnavn, brukernavn);
  const originalHilsen = sySammenHilsenTekst(veiledernavn);
  const [state, dispatch] = useReducer(reducer, { deletekst, originalHilsen }, initInitialState);
  const { navigerTilDialogen } = useNavigerTilDialogen();
  const fnr = useHentFnrFraUrl();

  const brukerdata = useHentBrukerdata();
  const manuellOppfolging = brukerdata.data?.manuellStatus?.erUnderManuellOppfolging;
  const krrStatusErReservert = brukerdata.data?.manuellStatus?.krrStatus?.erReservert;
  const kanVarsles = brukerdata?.data?.manuellStatus?.krrStatus?.kanVarsles;
  const kanIkkeDeleMedBruker = manuellOppfolging && krrStatusErReservert && !kanVarsles;
  const manuellStatus = !brukerdata.data?.manuellStatus;

  const feilmodal = manuellOppfolging || krrStatusErReservert || manuellStatus || kanIkkeDeleMedBruker;

  const clickCancel = (log = true) => {
    lukkModal();
    dispatch({ type: 'Avbryt' });
    log && logDelMedbrukerEvent('Avbrutt del med bruker');
  };

  return (
    <>
      {feilmodal ? (
        <KanIkkeDeleMedBrukerModal
          modalOpen={modalOpen}
          lukkModal={lukkModal}
          manuellOppfolging={manuellOppfolging!}
          kanIkkeDeleMedBruker={kanIkkeDeleMedBruker!}
          krrStatusErReservert={krrStatusErReservert!}
          manuellStatus={manuellStatus}
        />
      ) : (
        <Modal
          shouldCloseOnOverlayClick={false}
          closeButton={true}
          open={modalOpen}
          onClose={clickCancel}
          className={classNames(modalStyles.overstyrte_styles_fra_ds_modal, delemodalStyles.delemodal)}
          aria-label="modal"
        >
          <Modal.Content>
            {state.sendtStatus !== 'SENDT_OK' && state.sendtStatus !== 'SENDING_FEILET' && (
              <>
                <DelMedBrukerContent
                  tiltaksgjennomforingsnavn={tiltaksgjennomforingsnavn}
                  onCancel={clickCancel}
                  state={state}
                  dispatch={dispatch}
                  veiledernavn={veiledernavn}
                  brukernavn={brukernavn}
                />
                <BodyShort size="small">
                  Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen.
                </BodyShort>
              </>
            )}
          </Modal.Content>
        </Modal>
      )}
      {state.sendtStatus === 'SENDING_FEILET' && (
        <StatusModal
          modalOpen={modalOpen}
          ikonVariant="error"
          heading="Tiltaket kunne ikke deles"
          text={
            <>
              Tiltaket kunne ikke deles på grunn av en teknisk feil hos oss. Forsøk på nytt eller ta{' '}
              <a href={PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
            </>
          }
          onClose={clickCancel}
          primaryButtonOnClick={() => dispatch({ type: 'Reset' })}
          primaryButtonText="Prøv igjen"
          secondaryButtonOnClick={clickCancel}
          secondaryButtonText="Avbryt"
        />
      )}
      {state.sendtStatus === 'SENDT_OK' && (
        <StatusModal
          modalOpen={modalOpen}
          onClose={clickCancel}
          ikonVariant="success"
          heading="Tiltaket er delt med brukeren"
          text="Det er opprettet en ny tråd i Dialogen der du kan fortsette kommunikasjonen rundt dette tiltaket med brukeren."
          primaryButtonText="Gå til dialogen"
          primaryButtonOnClick={() => navigerTilDialogen(fnr, state.dialogId)}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={() => clickCancel(false)}
        />
      )}
    </>
  );
};
export default Delemodal;
