import {
  TilsagnAvvisningAarsak,
  TilsagnBesluttelseDto,
  TilsagnBesluttelseStatus,
  TilsagnDto,
} from "@mr/api-client";
import { Alert, Heading } from "@navikt/ds-react";
import { formaterDato } from "../../../utils/Utils";

interface Props {
  tilsagn: TilsagnDto;
}

export function AvvistDetaljer({ tilsagn }: Props) {
  const { besluttelse } = tilsagn;

  const aarsaker = besluttelse?.aarsaker?.map((aarsak) => tilsagnAarsakTilTekst(aarsak)) || [];

  return besluttelse?.status === TilsagnBesluttelseStatus.AVVIST && besluttelse?.aarsaker ? (
    <Alert variant="warning" size="small" style={{ marginTop: "1rem" }}>
      <Heading size="xsmall" level="3">
        Tilsagnet ble returnert
      </Heading>
      <p>
        {beslutternavn(besluttelse)} returnerte tilsagnet den {formaterDato(besluttelse.tidspunkt)}{" "}
        med følgende {aarsaker.length === 1 ? "årsak" : "årsaker"}:{" "}
        <b>{capitalizeFirstLetter(joinWithCommaAndOg(aarsaker))}</b>
        {besluttelse?.forklaring ? (
          <>
            {" "}
            med forklaringen: <b>"{besluttelse?.forklaring}"</b>
          </>
        ) : null}
        .
      </p>
    </Alert>
  ) : null;
}

function beslutternavn(besluttelse: TilsagnBesluttelseDto): string {
  return `${besluttelse.beslutternavn} ${besluttelse.navIdent}`;
}

type TilsagnAarsak =
  | "Feil periode"
  | "Feil antall plasser"
  | "Feil kostnadssted"
  | "Feil beløp"
  | "Annet";

function tilsagnAarsakTilTekst(aarsak: TilsagnAvvisningAarsak): TilsagnAarsak {
  switch (aarsak) {
    case TilsagnAvvisningAarsak.FEIL_PERIODE:
      return "Feil periode";
    case TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER:
      return "Feil antall plasser";
    case TilsagnAvvisningAarsak.FEIL_KOSTNADSSTED:
      return "Feil kostnadssted";
    case TilsagnAvvisningAarsak.FEIL_BELOP:
      return "Feil beløp";
    case TilsagnAvvisningAarsak.FEIL_ANNET:
      return "Annet";
  }
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
