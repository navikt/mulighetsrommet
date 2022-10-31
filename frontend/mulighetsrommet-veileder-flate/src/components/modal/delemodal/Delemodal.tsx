import { ErrorColored, SuccessColored } from '@navikt/ds-icons';
import { Alert, BodyShort, Button, Heading, Modal, Textarea } from '@navikt/ds-react';
import classNames from 'classnames';
import { useReducer, useRef } from 'react';
import { mulighetsrommetClient } from '../../../core/api/clients';
import { useFeatureToggles } from '../../../core/api/feature-toggles';
import { logEvent } from '../../../core/api/logger';
import { useHentDeltMedBrukerStatus } from '../../../core/api/queries/useHentDeltMedbrukerStatus';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { useNavigerTilDialogen } from '../../../hooks/useNavigerTilDialogen';
import { capitalize, erPreview } from '../../../utils/Utils';
import Lenke from '../../lenke/Lenke';
import modalStyles from '../Modal.module.scss';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';

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
    case 'Redigerer tekstfelt':
      return { ...state, redigererTekstfelt: true };
    case 'Tilbakestill tekstfelt':
      return initInitialState(state.malTekst);
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
    redigererTekstfelt: false,
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
    .replace('<Fornavn>', capitalize(brukerNavn))
    .replace('<tiltaksnavn>', tiltaksgjennomforingsnavn)}\n\nHilsen ${veiledernavn}`;
  const [state, dispatch] = useReducer(reducer, startText, initInitialState);
  const fnr = useHentFnrFraUrl();
  const features = useFeatureToggles();
  const skalLagreAtViDelerMedBruker =
    features.isSuccess && features.data['mulighetsrommet.lagre-del-tiltak-med-bruker'];
  const { lagreVeilederHarDeltTiltakMedBruker, refetch: refetchOmVeilederHarDeltMedBruker } =
    useHentDeltMedBrukerStatus();
  const { navigerTilDialogen } = useNavigerTilDialogen();
  const senderTilDialogen = state.sendtStatus === 'SENDER';
  const tekstfeltRef = useRef<HTMLTextAreaElement | null>(null);

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
    if (state.tekst.trim().length > getAntallTegn()) return;
    logDelMedbrukerEvent('Delte med bruker');

    dispatch({ type: 'Send melding' });
    const overskrift = `Tiltak gjennom NAV: ${tiltaksgjennomforingsnavn}`;
    const { tekst } = state;
    try {
      const res = await mulighetsrommetClient.dialogen.delMedDialogen({ fnr, requestBody: { overskrift, tekst } });
      if (skalLagreAtViDelerMedBruker) {
        // TODO Fjern sjekk og toggle mulighetsrommet.lagre-del-tiltak-med-bruker når vi har avklart med jurister at det er ok å lagre fnr til bruker i db
        await lagreVeilederHarDeltTiltakMedBruker(res.id);
        refetchOmVeilederHarDeltMedBruker();
      }
      dispatch({ type: 'Sendt ok', payload: res.id });
    } catch {
      dispatch({ type: 'Sending feilet' });
      logDelMedbrukerEvent('Del med bruker feilet');
    }
  };

  const clickCancel = (log = true) => {
    setModalOpen();
    dispatch({ type: 'Avbryt' });
    log && logDelMedbrukerEvent('Avbrutt del med bruker');
  };

  const gaTilDialogen = () => {
    navigerTilDialogen(fnr, state.dialogId);
  };

  const fokuserTekstfelt = () => {
    dispatch({ type: 'Redigerer tekstfelt' });
    logDelMedbrukerEvent('Redigerer tekstfelt');
    tekstfeltRef?.current?.focus();
  };

  const tilbakestillTekstfelt = () => {
    dispatch({ type: 'Tilbakestill tekstfelt' });
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
        {state.sendtStatus !== 'SENDT_OK' && state.sendtStatus !== 'SENDING_FEILET' && (
          <div>
            <p className={modalStyles.muted} data-testid="modal_header">
              Del med bruker
            </p>
            <Heading size="large" level="1">
              {'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
            </Heading>

            <Textarea
              value={state.tekst}
              onChange={e => dispatch({ type: 'Sett tekst', payload: e.currentTarget.value })}
              label=""
              minRows={10}
              maxRows={50}
              data-testid="textarea_tilbakemelding"
              maxLength={getAntallTegn()}
              error={handleError()}
              ref={tekstfeltRef}
              className={delemodalStyles.textarea}
            />
            <div className={modalStyles.modal_btngroup}>
              <div className={delemodalStyles.btn_row}>
                <Button
                  onClick={handleSend}
                  data-testid="modal_btn-send"
                  disabled={senderTilDialogen || state.tekst.length === 0 || erPreview}
                >
                  {senderTilDialogen ? 'Sender...' : 'Send via Dialogen'}
                </Button>

                <Button
                  variant="tertiary"
                  onClick={() => clickCancel()}
                  data-testid="modal_btn-cancel"
                  disabled={senderTilDialogen}
                >
                  Avbryt
                </Button>
              </div>
              <div>
                {state.redigererTekstfelt ? (
                  <Button
                    variant="tertiary"
                    onClick={tilbakestillTekstfelt}
                    data-testid="modal_btn-cancel"
                    disabled={senderTilDialogen}
                  >
                    Tilbakestill
                  </Button>
                ) : (
                  <Button
                    variant="tertiary"
                    onClick={fokuserTekstfelt}
                    data-testid="modal_btn-cancel"
                    disabled={senderTilDialogen}
                  >
                    Rediger melding
                  </Button>
                )}
              </div>
            </div>
            {erPreview && (
              <Alert variant="warning">
                Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn og veileders navn blir
                automatisk satt utenfor forhåndsvisningsmodus.
              </Alert>
            )}
          </div>
        )}
        {state.sendtStatus === 'SENDT_OK' && (
          <div className={delemodalStyles.delemodal_tilbakemelding}>
            <SuccessColored className={delemodalStyles.delemodal_svg} />
            <Heading level="1" size="large" data-testid="modal_header">
              Meldingen er sendt
            </Heading>
            <BodyShort>Du kan fortsette dialogen om dette tiltaket i Dialogen.</BodyShort>
            <div className={classNames(modalStyles.modal_btngroup, modalStyles.modal_btngroup_success)}>
              <Button variant="primary" onClick={gaTilDialogen} data-testid="modal_btn-dialog">
                Gå til Dialogen
              </Button>
              <Button variant="secondary" onClick={() => clickCancel(false)} data-testid="modal_btn-cancel">
                Lukk
              </Button>
            </div>
          </div>
        )}
        {state.sendtStatus === 'SENDING_FEILET' && (
          <div className={classNames(delemodalStyles.delemodal_tilbakemelding)}>
            <ErrorColored className={delemodalStyles.delemodal_svg} />
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
            <div className={modalStyles.modal_btngroup}>
              <Button variant="primary" onClick={() => dispatch({ type: 'Reset' })} data-testid="modal_btn-reset">
                Prøv på nytt
              </Button>
              <Button variant="secondary" onClick={() => clickCancel()} data-testid="modal_btn-cancel">
                Lukk
              </Button>
            </div>
          </div>
        )}
        <p className={modalStyles.muted}>
          Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen
        </p>
      </Modal.Content>
    </Modal>
  );
};
export default Delemodal;
