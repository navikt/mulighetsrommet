import { Alert, BodyShort, Button, ErrorMessage, Textarea } from '@navikt/ds-react';
import { DelMedBruker, SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';
import React, { Dispatch, useEffect, useRef, useState } from 'react';
import { erPreview, formaterDato } from '../../../utils/Utils';
import { logDelMedbrukerEvent } from './Delemodal';
import delemodalStyles from './Delemodal.module.scss';
import { Actions, State } from './DelemodalActions';

const MAKS_ANTALL_TEGN_HILSEN = 300;

interface Props {
  state: State;
  dispatch: Dispatch<Actions>;
  veiledernavn?: string;
  brukernavn?: string;
  harDeltMedBruker?: DelMedBruker;
  tiltaksgjennomforing: SanityTiltaksgjennomforing;
}

export function DelMedBrukerContent({
  state,
  dispatch,
  veiledernavn,
  brukernavn,
  harDeltMedBruker,
  tiltaksgjennomforing,
}: Props) {
  const [visPersonligMelding, setVisPersonligMelding] = useState(false);
  const personligHilsenRef = useRef<HTMLTextAreaElement>(null);
  const datoSidenSistDelt = harDeltMedBruker?.createdAt && formaterDato(new Date(harDeltMedBruker.createdAt));

  useEffect(() => {
    personligHilsenRef?.current?.focus();
  }, [visPersonligMelding]);

  const enablePersonligMelding = () => {
    dispatch({ type: 'Sett hilsen', payload: state.originalHilsen });
    setVisPersonligMelding(true);
    logDelMedbrukerEvent('Sett hilsen');
  };

  const handleError = () => {
    if (state.hilsen.length > MAKS_ANTALL_TEGN_HILSEN) return 'For mange tegn';
  };

  const redigerHilsen = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    dispatch({ type: 'Sett hilsen', payload: e.currentTarget.value });
  };

  return (
    <>
      {harDeltMedBruker ? (
        <Alert variant="warning">{`Dette tiltaket ble delt med bruker ${datoSidenSistDelt}.`}</Alert>
      ) : null}

      {visPersonligMelding && !state.deletekst ? null : (
        <BodyShort
          title="Teksten er hentet fra tiltakstypen og kan ikke redigeres."
          className={delemodalStyles.deletekst}
        >
          {`${state.deletekst}${visPersonligMelding ? '' : `\n\n${state.hilsen}`}`}
        </BodyShort>
      )}
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
        <>
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
          <Alert inline variant="info" className={delemodalStyles.personopplysninger}>
            Ikke del personopplysninger i din personlige hilsen
          </Alert>
        </>
      ) : null}
      {!veiledernavn ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>• Kunne ikke hente veileders navn</ErrorMessage>
      ) : null}
      {!brukernavn ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>• Kunne ikke hente brukers navn</ErrorMessage>
      ) : null}
      {!tiltaksgjennomforing?.tiltakstype?.delingMedBruker ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>
          • Mangler ferdigutfylt tekst som kan deles med bruker{' '}
        </ErrorMessage>
      ) : null}
      {erPreview ? (
        <Alert variant="warning" data-testid="alert-preview-del-med-bruker">
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn og veileders navn blir automatisk
          satt utenfor forhåndsvisningsmodus.
        </Alert>
      ) : null}
    </>
  );
}
