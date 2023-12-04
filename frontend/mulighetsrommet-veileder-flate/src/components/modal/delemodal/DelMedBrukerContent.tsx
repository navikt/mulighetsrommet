import { Alert, BodyShort, Button, ErrorMessage, Textarea } from "@navikt/ds-react";
import { DelMedBruker, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import React, { Dispatch, useEffect, useRef } from "react";
import { erPreview, formaterDato, hentBrukersFylkeOgLokalkontor } from "../../../utils/Utils";
import { logDelMedbrukerEvent } from "./Delemodal";
import delemodalStyles from "./Delemodal.module.scss";
import { Actions, State } from "./DelemodalActions";
import { useHentBrukerdata } from "../../../core/api/queries/useHentBrukerdata";

export const MAKS_ANTALL_TEGN_DEL_MED_BRUKER = 500;

interface Props {
  state: State;
  dispatch: Dispatch<Actions>;
  veiledernavn?: string;
  brukernavn?: string;
  harDeltMedBruker?: DelMedBruker;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

export function DelMedBrukerContent({
  state,
  dispatch,
  veiledernavn,
  brukernavn,
  harDeltMedBruker,
  tiltaksgjennomforing,
}: Props) {
  const { data: bruker } = useHentBrukerdata();
  const { skrivPersonligMelding, skrivPersonligIntro } = state;
  const personligIntroRef = useRef<HTMLTextAreaElement>(null);
  const personligHilsenRef = useRef<HTMLTextAreaElement>(null);
  const datoSidenSistDelt =
    harDeltMedBruker?.createdAt && formaterDato(new Date(harDeltMedBruker.createdAt));

  useEffect(() => {
    if (skrivPersonligIntro) {
      personligIntroRef?.current?.focus();
      return;
    }
    if (skrivPersonligMelding) {
      personligHilsenRef?.current?.focus();
    }
  }, [skrivPersonligMelding, skrivPersonligIntro]);

  const enablePersonligMelding = () => {
    dispatch({ type: "Skriv personlig melding", payload: true });
    dispatch({ type: "Sett hilsen", payload: state.originalHilsen });
    logDelMedbrukerEvent("Sett hilsen", hentBrukersFylkeOgLokalkontor(bruker));
  };

  const enablePersonligIntro = () => {
    dispatch({ type: "Skriv personlig intro", payload: true });
    dispatch({ type: "Sett intro", payload: "" });
    logDelMedbrukerEvent("Sett intro", hentBrukersFylkeOgLokalkontor(bruker));
  };

  const forMangeTegn = (tekst: string): boolean => {
    return tekst.length > MAKS_ANTALL_TEGN_DEL_MED_BRUKER;
  };

  const handleError = () => {
    if (forMangeTegn(state.hilsen) || forMangeTegn(state.introtekst)) return "For mange tegn";
  };

  const redigerHilsen = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    dispatch({ type: "Sett hilsen", payload: e.currentTarget.value });
  };

  const redigerIntro = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    dispatch({ type: "Sett intro", payload: e.currentTarget.value });
  };
  return (
    <>
      {harDeltMedBruker ? (
        <Alert variant="warning">{`Dette tiltaket ble delt med bruker ${datoSidenSistDelt}.`}</Alert>
      ) : null}

      {skrivPersonligIntro ? null : (
        <Button
          data-testid="personlig_intro_btn"
          onClick={enablePersonligIntro}
          variant="secondary"
          className={delemodalStyles.personlig_melding_btn}
        >
          Legg til personlig introduksjon
        </Button>
      )}

      {skrivPersonligIntro ? (
        <Textarea
          ref={personligIntroRef}
          className={delemodalStyles.personligHilsen}
          size="medium"
          value={state.introtekst}
          label=""
          hideLabel
          onChange={redigerIntro}
          maxLength={MAKS_ANTALL_TEGN_DEL_MED_BRUKER}
          data-testid="textarea_intro"
          error={handleError()}
        />
      ) : null}

      {skrivPersonligMelding && skrivPersonligIntro && !state.deletekst ? null : (
        <BodyShort
          title="Teksten er hentet fra tiltakstypen og kan ikke redigeres."
          className={delemodalStyles.deletekst}
        >
          {`${skrivPersonligIntro ? "" : `${state.introtekst}\n`}${state.deletekst}${
            skrivPersonligMelding ? "" : `\n\n${state.hilsen}`
          }`}
        </BodyShort>
      )}
      {skrivPersonligMelding ? null : (
        <Button
          data-testid="personlig_hilsen_btn"
          onClick={enablePersonligMelding}
          variant="secondary"
          className={delemodalStyles.personlig_melding_btn}
        >
          Legg til personlig melding
        </Button>
      )}

      {skrivPersonligMelding ? (
        <>
          <Textarea
            ref={personligHilsenRef}
            className={delemodalStyles.personligHilsen}
            size="medium"
            value={state.hilsen}
            label=""
            hideLabel
            onChange={redigerHilsen}
            maxLength={MAKS_ANTALL_TEGN_DEL_MED_BRUKER}
            data-testid="textarea_hilsen"
            error={handleError()}
          />
          <Alert inline variant="info" className={delemodalStyles.personopplysninger}>
            Ikke del personopplysninger i din personlige hilsen
          </Alert>
        </>
      ) : null}
      {!veiledernavn ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>
          • Kunne ikke hente veileders navn
        </ErrorMessage>
      ) : null}
      {!brukernavn ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>
          • Kunne ikke hente brukers navn
        </ErrorMessage>
      ) : null}
      {!tiltaksgjennomforing?.tiltakstype?.delingMedBruker ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>
          • Mangler ferdigutfylt tekst som kan deles med bruker{" "}
        </ErrorMessage>
      ) : null}
      {erPreview() ? (
        <Alert variant="warning" data-testid="alert-preview-del-med-bruker">
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn og veileders
          navn blir automatisk satt utenfor forhåndsvisningsmodus.
        </Alert>
      ) : null}
    </>
  );
}
