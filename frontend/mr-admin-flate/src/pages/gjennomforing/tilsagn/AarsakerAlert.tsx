import { Alert, Heading } from "@navikt/ds-react";
import { formaterDato, tilsagnAarsakTilTekst } from "../../../utils/Utils";
import {
  TilsagnAvvisningAarsak,
  TilsagnTilAnnulleringAarsak,
  ToTrinnskontrollHandlingDto,
} from "@mr/api-client-v2";

export function AvvistAlert({ handling }: { handling: ToTrinnskontrollHandlingDto }) {
  const aarsaker =
    handling.aarsaker?.map((aarsak) =>
      tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak | TilsagnTilAnnulleringAarsak),
    ) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet ble returnert
      </Heading>
      <p>
        {handling.opprettetAvNavn} {handling.opprettetAv} returnerte tilsagnet den{" "}
        {formaterDato(handling.createdAt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {handling.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{handling.forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  );
}

export function TilAnnulleringAlert({ handling }: { handling: ToTrinnskontrollHandlingDto }) {
  const aarsaker =
    handling.aarsaker?.map((aarsak) =>
      tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak | TilsagnTilAnnulleringAarsak),
    ) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet annulleres
      </Heading>
      <p>
        {handling.opprettetAvNavn} {handling.opprettetAv} sendte tilsagnet til annullering
        den {formaterDato(handling.createdAt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {handling.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{handling.forklaring}"</b>
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
