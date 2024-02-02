import { Alert, Button, ErrorMessage, Textarea } from "@navikt/ds-react";
import { DelMedBruker, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import React, { Dispatch, useEffect, useRef } from "react";
import { erPreview, formaterDato } from "@/utils/Utils";
import delemodalStyles from "./Delemodal.module.scss";
import { Actions, State } from "./DelemodalActions";
import { useLogEvent } from "@/logging/amplitude";
import { getDelMedBrukerTekst } from "@/apps/modia/delMedBruker/helpers";

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
  const { enableRedigerDeletekst } = state;
  const endreDeletekstRef = useRef<HTMLTextAreaElement>(null);
  const datoSidenSistDelt =
    harDeltMedBruker?.createdAt && formaterDato(new Date(harDeltMedBruker.createdAt));
  const { logEvent } = useLogEvent();

  const standardtekstLengde = state.deletekst.length;

  useEffect(() => {
    if (enableRedigerDeletekst) {
      endreDeletekstRef?.current?.focus();
    }
  }, [enableRedigerDeletekst]);

  const enableEndreDeletekst = () => {
    dispatch({ type: "Enable rediger deletekst", payload: true });
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Endre deletekst", tiltakstype: tiltaksgjennomforing.tiltakstype.navn },
    });
  };

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
        <Alert variant="warning">{`Dette tiltaket ble delt med bruker ${datoSidenSistDelt}.`}</Alert>
      ) : null}

      <Textarea
        label="Tekst som deles med bruker"
        hideLabel
        readOnly={!enableRedigerDeletekst}
        className={delemodalStyles.deletekst}
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

      {enableRedigerDeletekst ? null : (
        <Button
          data-testid="endre-deletekst_btn"
          onClick={enableEndreDeletekst}
          variant="secondary"
          className={delemodalStyles.endreTekst_btn}
        >
          Endre melding
        </Button>
      )}

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

      {!getDelMedBrukerTekst(tiltaksgjennomforing) ? (
        <ErrorMessage className={delemodalStyles.feilmeldinger}>
          • Mangler ferdigutfylt tekst som kan deles med bruker{" "}
        </ErrorMessage>
      ) : null}

      {erPreview() ? (
        <Alert
          variant="warning"
          data-testid="alert-preview-del-med-bruker"
          className={delemodalStyles.preview_alert}
        >
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn blir automatisk
          satt utenfor forhåndsvisningsmodus.
        </Alert>
      ) : null}
    </>
  );
}
