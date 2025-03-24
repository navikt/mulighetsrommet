import {
  TilsagnAvvisningAarsak,
  TilsagnTilAnnulleringAarsak,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { Alert, Heading } from "@navikt/ds-react";
import { formaterDato, tilsagnAarsakTilTekst } from "@/utils/Utils";

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
  const aarsakTekster =
    aarsaker?.map((aarsak) => tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak)) || [];
  return (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        {header}
      </Heading>
      <p>
        {navIdent} avviste {entitet} den {formaterDato(tidspunkt)} med følgende{" "}
        {aarsakTekster && aarsakTekster.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsakTekster ?? []))}</b>
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
