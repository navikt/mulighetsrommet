import { Alert, ErrorMessage, Textarea } from "@navikt/ds-react";
import { DeltMedBrukerDto, VeilederflateTiltak } from "@api-client";
import React, { Dispatch, useEffect, useRef } from "react";
import { erPreview, formaterDato } from "@/utils/Utils";
import { Actions, State } from "./DelemodalActions";
import { getDelMedBrukerTekst } from "@/apps/modia/delMedBruker/helpers";
import Markdown from "react-markdown";

export const MAKS_ANTALL_TEGN_DEL_MED_BRUKER = 500;

interface Props {
  state: State;
  dispatch: Dispatch<Actions>;
  veiledernavn?: string;
  brukernavn: string | null;
  deltMedBruker?: DeltMedBrukerDto;
  tiltak: VeilederflateTiltak;
  enableRedigerDeletekst: boolean;
}

export function DelMedBrukerContent({
  state,
  dispatch,
  veiledernavn,
  brukernavn,
  deltMedBruker,
  tiltak,
  enableRedigerDeletekst,
}: Props) {
  const endreDeletekstRef = useRef<HTMLTextAreaElement>(null);
  const datoSidenSistDelt = deltMedBruker
    ? formaterDato(new Date(deltMedBruker.deling.tidspunkt))
    : null;

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
      {deltMedBruker ? (
        <Alert
          variant="warning"
          className="mb-4"
        >{`Dette tiltaket ble delt med bruker ${datoSidenSistDelt}.`}</Alert>
      ) : null}

      {!enableRedigerDeletekst ? (
        <div
          className="prose min-w-full bg-surface-subtle border border-border-subtle p-2"
          data-testid="textarea_deletekst"
        >
          <Markdown>{state.deletekst}</Markdown>
        </div>
      ) : (
        <Textarea
          label="Tekst som deles med bruker"
          hideLabel
          className="whitespace-pre-wrap rounded-lg p-1 prose min-w-full"
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
        <ErrorMessage className="mt-2 mb-4">• Kunne ikke hente veileders navn</ErrorMessage>
      ) : null}

      {!brukernavn ? (
        <ErrorMessage className="mt-2 mb-4">• Kunne ikke hente brukers navn</ErrorMessage>
      ) : null}

      {!getDelMedBrukerTekst(tiltak) ? (
        <ErrorMessage className="mt-2 mb-4">
          • Mangler ferdigutfylt tekst som kan deles med bruker{" "}
        </ErrorMessage>
      ) : null}

      {erPreview() ? (
        <Alert variant="warning" data-testid="alert-preview-del-med-bruker" className="mt-4">
          Det er ikke mulig å dele tiltak med bruker i forhåndsvisning. Brukers navn blir automatisk
          satt utenfor forhåndsvisningsmodus.
        </Alert>
      ) : null}
    </>
  );
}
