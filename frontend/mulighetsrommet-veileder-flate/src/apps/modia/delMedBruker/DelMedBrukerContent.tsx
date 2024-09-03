import { Alert, ErrorMessage, Textarea } from "@navikt/ds-react";
import { DelMedBruker, VeilederflateTiltak } from "@mr/api-client";
import React, { Dispatch, useEffect, useRef } from "react";
import { erPreview, formaterDato } from "@/utils/Utils";
import styles from "./Delemodal.module.scss";
import { Actions, State } from "./DelemodalActions";
import { getDelMedBrukerTekst } from "@/apps/modia/delMedBruker/helpers";
import Markdown from "react-markdown";

export const MAKS_ANTALL_TEGN_DEL_MED_BRUKER = 500;

interface Props {
  state: State;
  dispatch: Dispatch<Actions>;
  veiledernavn?: string;
  brukernavn?: string;
  harDeltMedBruker?: DelMedBruker;
  tiltak: VeilederflateTiltak;
  enableRedigerDeletekst: boolean;
}

export function DelMedBrukerContent({
  state,
  dispatch,
  veiledernavn,
  brukernavn,
  harDeltMedBruker,
  tiltak,
  enableRedigerDeletekst,
}: Props) {
  const endreDeletekstRef = useRef<HTMLTextAreaElement>(null);
  const datoSidenSistDelt =
    harDeltMedBruker?.createdAt && formaterDato(new Date(harDeltMedBruker.createdAt));

  const standardtekstLengde = state.deletekst.length;

  useEffect(() => {
    if (enableRedigerDeletekst) {
      endreDeletekstRef?.current?.focus();
    }
  }, [enableRedigerDeletekst]);

  const forMangeTegn = (tekst: string): boolean => {
    return tekst.length > standardtekstLengde + MAKS_ANTALL_TEGN_DEL_MED_BRUKER;
  };

  const handleError = () => {
    if (forMangeTegn(state.deletekst)) return "For mange tegn";
  };

  const redigerDeletekst = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    dispatch({ type: "Set deletekst", payload: e.currentTarget.value });
  };

  return (
    <>
      {harDeltMedBruker ? (
        <Alert
          variant="warning"
          className={styles.top_warning}
        >{`Dette tiltaket ble delt med bruker ${datoSidenSistDelt}.`}</Alert>
      ) : null}

      {!enableRedigerDeletekst ? (
        <div className={styles.markdown} data-testid="textarea_deletekst">
          <Markdown>{state.deletekst}</Markdown>
        </div>
      ) : (
        <Textarea
          label="Tekst som deles med bruker"
          hideLabel
          className={styles.deletekst}
          error={handleError()}
          ref={endreDeletekstRef}
          size="medium"
          onChange={redigerDeletekst}
          data-testid="textarea_deletekst"
          maxLength={state.originalDeletekst.length + MAKS_ANTALL_TEGN_DEL_MED_BRUKER}
          value={state.deletekst}
        >
          {state.deletekst}
        </Textarea>
      )}

      {!veiledernavn ? (
        <ErrorMessage className={styles.feilmeldinger}>
          • Kunne ikke hente veileders navn
        </ErrorMessage>
      ) : null}

      {!brukernavn ? (
        <ErrorMessage className={styles.feilmeldinger}>
          • Kunne ikke hente brukers navn
        </ErrorMessage>
      ) : null}

      {!getDelMedBrukerTekst(tiltak) ? (
        <ErrorMessage className={styles.feilmeldinger}>
          • Mangler ferdigutfylt tekst som kan deles med bruker{" "}
        </ErrorMessage>
      ) : null}

      {erPreview() ? (
        <Alert
          variant="warning"
          data-testid="alert-preview-del-med-bruker"
          className={styles.preview_alert}
        >
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn blir automatisk
          satt utenfor forhåndsvisningsmodus.
        </Alert>
      ) : null}
    </>
  );
}
