import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react';
import { Bruker, DelMedBruker, VeilederflateTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { PORTEN } from 'mulighetsrommet-frontend-common/constants';
import { useReducer } from 'react';
import { mulighetsrommetClient } from '../../../core/api/clients';
import { logEvent } from '../../../core/api/logger';
import { useHentDeltMedBrukerStatus } from '../../../core/api/queries/useHentDeltMedbrukerStatus';
import { byttTilDialogFlate } from '../../../utils/DialogFlateUtils';
import { capitalize, erPreview } from '../../../utils/Utils';
import modalStyles from '../Modal.module.scss';
import { StatusModal } from '../StatusModal';
import { DelMedBrukerContent } from './DelMedBrukerContent';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';
import { KanIkkeDeleMedBrukerModal } from './KanIkkeDeleMedBrukerModal';

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
  brukerFnr: string;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  brukerdata: Bruker;
  harDeltMedBruker?: DelMedBruker;
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
    .replace(' <Fornavn>', brukernavn ? ` ${brukernavn}` : '')
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
  brukerFnr,
  tiltaksgjennomforing,
  brukerdata,
  harDeltMedBruker,
}: DelemodalProps) => {
  const deletekst = sySammenBrukerTekst(chattekst, tiltaksgjennomforingsnavn, brukernavn);
  const originalHilsen = sySammenHilsenTekst(veiledernavn);
  const [state, dispatch] = useReducer(reducer, { deletekst, originalHilsen }, initInitialState);

  const manuellOppfolging = brukerdata?.manuellStatus?.erUnderManuellOppfolging;
  const krrStatusErReservert = brukerdata?.manuellStatus?.krrStatus?.erReservert;
  const kanVarsles = brukerdata?.manuellStatus?.krrStatus?.kanVarsles;
  const kanIkkeDeleMedBruker = manuellOppfolging && krrStatusErReservert && !kanVarsles;
  const manuellStatus = !brukerdata?.manuellStatus;
  const feilmodal = manuellOppfolging || krrStatusErReservert || manuellStatus || kanIkkeDeleMedBruker;
  const senderTilDialogen = state.sendtStatus === 'SENDER';
  const tiltaksgjennomforingId = tiltaksgjennomforing?._id.toString();
  const { lagreVeilederHarDeltTiltakMedBruker } = useHentDeltMedBrukerStatus(tiltaksgjennomforing?._id, brukerFnr);
  const MAKS_ANTALL_TEGN_HILSEN = 300;

  const clickCancel = (log = true) => {
    lukkModal();
    dispatch({ type: 'Avbryt' });
    log && logDelMedbrukerEvent('Avbrutt del med bruker');
  };

  const getAntallTegn = () => {
    return state.hilsen.length;
  };

  const sySammenDeletekst = () => {
    return `${state.deletekst}\n\n${state.hilsen}`;
  };

  const handleSend = async () => {
    if (state.hilsen.trim().length > getAntallTegn()) return;
    logDelMedbrukerEvent('Delte med bruker');

    dispatch({ type: 'Send melding' });
    const overskrift = `Tiltak gjennom NAV: ${tiltaksgjennomforingsnavn}`;
    const tekst = sySammenDeletekst();
    try {
      const res = await mulighetsrommetClient.dialogen.delMedDialogen({
        fnr: brukerFnr,
        requestBody: { overskrift, tekst },
      });
      if (tiltaksgjennomforingId) {
        await lagreVeilederHarDeltTiltakMedBruker(res.id, tiltaksgjennomforingId);
      }
      dispatch({ type: 'Sendt ok', payload: res.id });
    } catch {
      dispatch({ type: 'Sending feilet' });
      logDelMedbrukerEvent('Del med bruker feilet');
    }
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
        <Modal open={modalOpen} onClose={() => clickCancel()} className={delemodalStyles.delemodal} aria-label="modal">
          <Modal.Header closeButton data-testid="modal_header">
            <Heading size="xsmall">Del med bruker</Heading>
            <Heading size="large" level="1" className={delemodalStyles.heading}>
              {'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
            </Heading>
          </Modal.Header>
          <Modal.Body>
            {state.sendtStatus !== 'SENDT_OK' && state.sendtStatus !== 'SENDING_FEILET' && (
              <DelMedBrukerContent
                state={state}
                dispatch={dispatch}
                veiledernavn={veiledernavn}
                brukernavn={brukernavn}
                harDeltMedBruker={harDeltMedBruker}
                tiltaksgjennomforing={tiltaksgjennomforing}
              />
            )}
          </Modal.Body>
          <Modal.Footer>
            <div className={modalStyles.knapperad}>
              <Button
                variant="tertiary"
                onClick={() => clickCancel(true)}
                data-testid="modal_btn-cancel"
                disabled={senderTilDialogen}
              >
                Avbryt
              </Button>
              <Button
                onClick={handleSend}
                data-testid="modal_btn-send"
                disabled={
                  senderTilDialogen ||
                  state.hilsen.length === 0 ||
                  state.hilsen.length > MAKS_ANTALL_TEGN_HILSEN ||
                  erPreview
                }
              >
                {senderTilDialogen ? 'Sender...' : 'Send via Dialogen'}
              </Button>
            </div>
            <BodyShort size="small">
              Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen.
            </BodyShort>
          </Modal.Footer>
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
          primaryButtonOnClick={event => byttTilDialogFlate({ event, dialogId: state.dialogId })}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={() => clickCancel(false)}
        />
      )}
    </>
  );
};
export default Delemodal;
