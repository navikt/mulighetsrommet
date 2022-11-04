import { Alert, Button, ErrorMessage, Heading, Textarea } from '@navikt/ds-react';
import classNames from 'classnames';
import { Dispatch, useRef } from 'react';
import { mulighetsrommetClient } from '../../../core/api/clients';
import { useFeatureToggles } from '../../../core/api/feature-toggles';
import { useHentDeltMedBrukerStatus } from '../../../core/api/queries/useHentDeltMedbrukerStatus';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { erPreview } from '../../../utils/Utils';
import modalStyles from '../Modal.module.scss';
import { logDelMedbrukerEvent } from './Delemodal';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';

interface Props {
  tiltaksgjennomforingsnavn: string;
  startTekst: string;
  onCancel: () => void;
  state: State;
  dispatch: Dispatch<Actions>;
  veiledernavn?: string;
  brukernavn?: string;
}

export function DelMedBrukerContent({
  tiltaksgjennomforingsnavn,
  startTekst,
  onCancel,
  state,
  dispatch,
  veiledernavn,
  brukernavn,
}: Props) {
  const senderTilDialogen = state.sendtStatus === 'SENDER';
  const tekstfeltRef = useRef<HTMLTextAreaElement | null>(null);
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const tiltaksnummer = tiltaksgjennomforing?.tiltaksnummer?.toString();
  const features = useFeatureToggles();
  const skalLagreAtViDelerMedBruker =
    features.isSuccess && features.data['mulighetsrommet.lagre-del-tiltak-med-bruker'];
  const { lagreVeilederHarDeltTiltakMedBruker, refetch: refetchOmVeilederHarDeltMedBruker } =
    useHentDeltMedBrukerStatus();
  const fnr = useHentFnrFraUrl();

  const getAntallTegn = () => {
    if (startTekst.length === 0) {
      return 750;
    }
    return startTekst.length + 200;
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
      if (skalLagreAtViDelerMedBruker && tiltaksnummer) {
        await lagreVeilederHarDeltTiltakMedBruker(res.id, tiltaksnummer);
        refetchOmVeilederHarDeltMedBruker();
      }
      dispatch({ type: 'Sendt ok', payload: res.id });
    } catch {
      dispatch({ type: 'Sending feilet' });
      logDelMedbrukerEvent('Del med bruker feilet');
    }
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
    <div>
      <p className={classNames(modalStyles.muted, modalStyles.mt_0)} data-testid="modal_header">
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
      {!veiledernavn && (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>• Kunne ikke hente veileders navn</ErrorMessage>
      )}
      {!brukernavn && (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>• Kunne ikke hente brukers navn</ErrorMessage>
      )}
      <div className={modalStyles.modal_btngroup}>
        <div className={delemodalStyles.btn_row}>
          <Button
            onClick={handleSend}
            data-testid="modal_btn-send"
            disabled={senderTilDialogen || state.tekst.length === 0 || erPreview}
          >
            {senderTilDialogen ? 'Sender...' : 'Send via Dialogen'}
          </Button>

          <Button variant="tertiary" onClick={onCancel} data-testid="modal_btn-cancel" disabled={senderTilDialogen}>
            Avbryt
          </Button>
        </div>
        <div>
          {state.redigererTekstfelt || state.tekst != state.malTekst ? ( //eller hvis tekst er endret
            <Button
              variant="tertiary"
              onClick={tilbakestillTekstfelt}
              data-testid="del-med-bruker_btn_tilbakestill"
              disabled={senderTilDialogen}
            >
              Tilbakestill
            </Button>
          ) : (
            <Button
              variant="tertiary"
              onClick={fokuserTekstfelt}
              data-testid="del-med-bruker_btn_rediger-melding"
              disabled={senderTilDialogen}
            >
              Rediger melding
            </Button>
          )}
        </div>
      </div>
      {erPreview && (
        <Alert variant="warning" data-testid="alert-preview-del-med-bruker">
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn og veileders navn blir automatisk
          satt utenfor forhåndsvisningsmodus.
        </Alert>
      )}
    </div>
  );
}
