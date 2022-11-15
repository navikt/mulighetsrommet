import { Alert, Button, ErrorMessage, Heading, Textarea } from '@navikt/ds-react';
import classNames from 'classnames';
import React, { Dispatch, useEffect, useRef, useState } from 'react';
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

const MAKS_ANTALL_TEGN_HILSEN = 300;

interface Props {
  tiltaksgjennomforingsnavn: string;
  onCancel: () => void;
  state: State;
  dispatch: Dispatch<Actions>;
  veiledernavn?: string;
  brukernavn?: string;
}

export function DelMedBrukerContent({
  tiltaksgjennomforingsnavn,
  onCancel,
  state,
  dispatch,
  veiledernavn,
  brukernavn,
}: Props) {
  const [visPersonligMelding, setVisPersonligMelding] = useState(false);
  const senderTilDialogen = state.sendtStatus === 'SENDER';
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const tiltaksnummer = tiltaksgjennomforing?.tiltaksnummer?.toString();
  const features = useFeatureToggles();
  const skalLagreAtViDelerMedBruker =
    features.isSuccess && features.data['mulighetsrommet.lagre-del-tiltak-med-bruker'];
  const { lagreVeilederHarDeltTiltakMedBruker, refetch: refetchOmVeilederHarDeltMedBruker } =
    useHentDeltMedBrukerStatus();
  const fnr = useHentFnrFraUrl();
  const personligHilsenRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    personligHilsenRef?.current?.focus();
  }, [visPersonligMelding]);

  const getAntallTegn = () => {
    return state.hilsen.length;
  };

  const sySammenDeletekst = () => {
    return `${state.deletekst}\n\n${state.hilsen}`;
  };

  const enablePersonligMelding = () => {
    dispatch({ type: 'Sett hilsen', payload: state.originalHilsen });
    setVisPersonligMelding(true);
  };

  const handleError = () => {
    if (state.hilsen.length > MAKS_ANTALL_TEGN_HILSEN) return 'For mange tegn';
  };

  const handleSend = async () => {
    if (state.hilsen.trim().length > getAntallTegn()) return;
    logDelMedbrukerEvent('Delte med bruker');

    dispatch({ type: 'Send melding' });
    const overskrift = `Tiltak gjennom NAV: ${tiltaksgjennomforingsnavn}`;
    const tekst = sySammenDeletekst();
    try {
      const res = await mulighetsrommetClient.dialogen.delMedDialogen({ requestBody: { overskrift, tekst } });
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

  const redigerHilsen = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    dispatch({ type: 'Sett hilsen', payload: e.currentTarget.value });
  };

  return (
    <div className={delemodalStyles.container}>
      <p className={classNames(modalStyles.muted, modalStyles.mt_0)} data-testid="modal_header">
        Del med bruker
      </p>
      <Heading size="large" level="1">
        {'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
      </Heading>

      <p title="Teksten er hentet fra tiltakstypen og kan ikke redigeres." className={delemodalStyles.deletekst}>
        {`${state.deletekst}${visPersonligMelding ? '' : `\n\n${state.hilsen}`}`}
      </p>
      {visPersonligMelding ? null : (
        <Button data-testid="personlig_hilsen_btn" onClick={enablePersonligMelding} variant="tertiary">
          Legg til personlig melding{' '}
        </Button>
      )}

      {visPersonligMelding ? (
        <Textarea
          ref={personligHilsenRef}
          className={delemodalStyles.personligHilsen}
          size="medium"
          value={state.hilsen}
          label=""
          hideLabel
          onChange={redigerHilsen}
          maxLength={MAKS_ANTALL_TEGN_HILSEN}
          data-testid="textarea_hilsen"
          error={handleError()}
        />
      ) : null}
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
            disabled={senderTilDialogen || state.hilsen.length > MAKS_ANTALL_TEGN_HILSEN || erPreview}
          >
            {senderTilDialogen ? 'Sender...' : 'Send via Dialogen'}
          </Button>

          <Button variant="tertiary" onClick={onCancel} data-testid="modal_btn-cancel" disabled={senderTilDialogen}>
            Avbryt
          </Button>
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
