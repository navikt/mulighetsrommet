import { TilsagnTilAnnulleringAarsak, Totrinnskontroll } from "@mr/api-client-v2";
import { Alert, Heading } from "@navikt/ds-react";
import { formaterDato, tilsagnAarsakTilTekst } from "../../../utils/Utils";

export function TilAnnulleringAlert({ annullering }: { annullering: Totrinnskontroll }) {
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

interface Props {
  header: string;
  aarsaker: string[];
  tidspunkt?: string;
  forklaring?: string;
  navIdent?: string;
}

export function AvvistAlert({ aarsaker, forklaring, navIdent, tidspunkt, header }: Props) {
  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        {header}
      </Heading>
      <p>
        {navIdent} avviste den {formaterDato(tidspunkt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
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

function joinWithCommaAndOg(aarsaker: string[]): string {
  if (aarsaker.length === 0) return "";
  if (aarsaker.length === 1) return aarsaker[0];
  return `${aarsaker.slice(0, -1).join(", ")} og ${aarsaker[aarsaker.length - 1]}`;
}

function capitalizeFirstLetter(text: string): string {
  if (!text) return "";
  return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
}
