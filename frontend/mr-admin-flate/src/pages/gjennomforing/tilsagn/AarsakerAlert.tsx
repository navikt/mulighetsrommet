import {
  capitalizeFirstLetter,
  formaterDato,
  joinWithCommaAndOg,
  tilsagnAarsakTilTekst,
} from "@/utils/Utils";
import { TilsagnTilAnnulleringAarsak, TotrinnskontrollDto } from "@mr/api-client-v2";
import { Alert, Heading } from "@navikt/ds-react";

export function TilAnnulleringAlert({ annullering }: { annullering: TotrinnskontrollDto }) {
  const aarsaker =
    annullering.aarsaker?.map((aarsak) =>
      tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
    ) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet annulleres
      </Heading>
      <p>
        {annullering.behandletAv} sendte tilsagnet til annullering den{" "}
        {formaterDato(annullering.behandletTidspunkt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {annullering.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{annullering.forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  );
}

export function TilOppgjorAlert({ oppgjor }: { oppgjor: TotrinnskontrollDto }) {
  const aarsaker =
    oppgjor.aarsaker?.map((aarsak) =>
      tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
    ) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet gjøres opp
      </Heading>
      <p>
        {oppgjor.behandletAv} sendte tilsagnet til oppgjør den{" "}
        {formaterDato(oppgjor.behandletTidspunkt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {oppgjor.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{oppgjor.forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  );
}

interface Props {
  header: string;
  aarsaker?: string[];
  tidspunkt?: string;
  forklaring?: string;
  navIdent?: string;
  entitet: "utbetalingen" | "tilsagnet";
}

export function AvvistAlert({ aarsaker, forklaring, navIdent, tidspunkt, header, entitet }: Props) {
  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        {header}
      </Heading>
      <p>
        {navIdent} returnerte {entitet} den {formaterDato(tidspunkt)} med følgende{" "}
        {aarsaker && aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker ?? []))}</b>
        {forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  );
}
