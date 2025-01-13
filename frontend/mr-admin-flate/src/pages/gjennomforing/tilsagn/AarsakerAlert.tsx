import { TilsagnStatusReturnert, TilsagnStatusTilAnnullering } from "@mr/api-client";
import { Alert, Heading } from "@navikt/ds-react";
import { formaterDato, tilsagnAarsakTilTekst } from "../../../utils/Utils";

export function AvvistAlert({ status }: { status: TilsagnStatusReturnert }) {
  const aarsaker = status?.aarsaker?.map((aarsak) => tilsagnAarsakTilTekst(aarsak)) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet ble returnert
      </Heading>
      <p>
        {status.returnertAvNavn} {status.returnertAv} returnerte tilsagnet den{" "}
        {formaterDato(status.endretTidspunkt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {status?.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{status?.forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  );
}

export function TilAnnulleringAlert({ status }: { status: TilsagnStatusTilAnnullering }) {
  const aarsaker = status?.aarsaker?.map((aarsak) => tilsagnAarsakTilTekst(aarsak)) || [];

  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet annulleres
      </Heading>
      <p>
        {status.endretAvNavn} {status.endretAv} sendte tilsagnet til annullering den{" "}
        {formaterDato(status.endretTidspunkt)} med følgende{" "}
        {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {status?.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{status?.forklaring}"</b>
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
