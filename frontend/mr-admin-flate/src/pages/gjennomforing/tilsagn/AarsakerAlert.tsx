import { Alert, Heading } from "@navikt/ds-react";
import { formaterDato, tilsagnAarsakTilTekst } from "../../../utils/Utils";
import {
  TilsagnAvvisningAarsak,
  TilsagnTilAnnulleringAarsak,
  ToTrinnskontroll,
} from "@mr/api-client-v2";

export function AvvistAlert({ godkjennelse }: { godkjennelse: ToTrinnskontroll }) {
  const aarsaker =
    godkjennelse.aarsaker?.map((aarsak) =>
      tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak | TilsagnTilAnnulleringAarsak),
    ) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet ble returnert
      </Heading>
      <p>
        {godkjennelse.beslutt?.navn} {godkjennelse.beslutt?.navIdent} returnerte tilsagnet den{" "}
        {formaterDato(godkjennelse.beslutt?.tidspunkt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {godkjennelse.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{godkjennelse.forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  );
}

export function TilAnnulleringAlert({ annullering }: { annullering: ToTrinnskontroll }) {
  const aarsaker =
    annullering.aarsaker?.map((aarsak) =>
      tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak | TilsagnTilAnnulleringAarsak),
    ) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet annulleres
      </Heading>
      <p>
        {annullering.opprett.navn} {annullering.opprett.navIdent} sendte tilsagnet til annullering
        den {formaterDato(annullering.opprett.tidspunkt)} med følgende{" "}
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

function joinWithCommaAndOg(aarsaker: string[]): string {
  if (aarsaker.length === 0) return "";
  if (aarsaker.length === 1) return aarsaker[0];
  return `${aarsaker.slice(0, -1).join(", ")} og ${aarsaker[aarsaker.length - 1]}`;
}

function capitalizeFirstLetter(text: string): string {
  if (!text) return "";
  return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
}
