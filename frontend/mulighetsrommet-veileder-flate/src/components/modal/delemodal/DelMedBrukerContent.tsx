import { Alert, BodyShort, Button, ErrorMessage, Heading, Textarea } from '@navikt/ds-react';
import classNames from 'classnames';
import React, { Dispatch, useEffect, useRef, useState } from 'react';
import { mulighetsrommetClient } from '../../../core/api/clients';
import { useHentDeltMedBrukerStatus } from '../../../core/api/queries/useHentDeltMedbrukerStatus';
import useTiltaksgjennomforingById from '../../../core/api/queries/useTiltaksgjennomforingById';
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
  const { lagreVeilederHarDeltTiltakMedBruker, refetch: refetchOmVeilederHarDeltMedBruker } =
    useHentDeltMedBrukerStatus();
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
      if (tiltaksnummer) {
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
      <Heading
        size="xsmall"
        level="2"
        className={classNames(modalStyles.muted, modalStyles.mt_0)}
        data-testid="modal_header"
      >
        Del med bruker
      </Heading>
      <Heading size="large" level="1">
        {'Tiltak gjennom NAV: ' + tiltaksgjennomforingsnavn}
      </Heading>

      <BodyShort
        title="Teksten er hentet fra tiltakstypen og kan ikke redigeres."
        className={delemodalStyles.deletekst}
      >
        {`${state.deletekst}${visPersonligMelding ? '' : `\n\n${state.hilsen}`}`}
      </BodyShort>
      {visPersonligMelding ? null : (
        <Button
          data-testid="personlig_hilsen_btn"
          onClick={enablePersonligMelding}
          variant="secondary"
          className={delemodalStyles.personlig_melding_btn}
        >
          Legg til personlig melding
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
      {!tiltaksgjennomforing?.tiltakstype?.delingMedBruker && (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>
          • Klarte ikke hente preutfylt tekst om tiltaket{' '}
        </ErrorMessage>
      )}
      <div className={classNames(modalStyles.modal_btngroup, delemodalStyles.btn_row)}>
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
      {erPreview && (
        <Alert variant="warning" data-testid="alert-preview-del-med-bruker">
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn og veileders navn blir automatisk
          satt utenfor forhåndsvisningsmodus.
        </Alert>
      )}
    </div>
  );
}
