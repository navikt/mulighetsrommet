import { TilsagnStatus, TilsagnStatusOgAarsaker, TilsagnTilAnnulleringAarsak } from "api-client";
import { HelpText, List, Tag } from "@navikt/ds-react";

interface Props {
  data: TilsagnStatusOgAarsaker;
}

export function TilsagnStatusTag({ data }: Props) {
  const aarsaker = data.aarsaker || [];
  switch (data.status) {
    case TilsagnStatus.GODKJENT:
      return (
        <Tag variant="success" className="w-max">
          Godkjent
        </Tag>
      );
    case TilsagnStatus.ANNULLERT:
      return (
        <Tag variant="error" className="line-through bg-white! text-text-danger! w-max">
          Annullert {skalViseHelptext("Tilsagnet er annullert med følgende årsak(er):", aarsaker)}
        </Tag>
      );
    case TilsagnStatus.TIL_ANNULLERING:
      return (
        <Tag variant="warning" className="bg-white! border-text-danger w-max">
          Til annullering{" "}
          {skalViseHelptext(
            "Tilsagnet er til annullering hos NAV med følgende årsak(er):",
            aarsaker,
          )}
        </Tag>
      );
    case TilsagnStatus.TIL_OPPGJOR:
      return (
        <Tag variant="error" className="bg-white! w-max">
          Til oppgjør
        </Tag>
      );

    case TilsagnStatus.OPPGJORT:
      return (
        <Tag variant="neutral" className="w-max">
          Oppgjort
        </Tag>
      );

    default:
      return null;
  }
}

function skalViseHelptext(beskrivelse: string, aarsaker: TilsagnTilAnnulleringAarsak[]) {
  if (aarsaker.length > 0) {
    return (
      <HelpText className="ml-2">
        <span className="text-text-default">
          {beskrivelse}{" "}
          <List>
            {aarsaker.map(aarsakTilTekst).map((aarsak) => (
              <List.Item key={aarsak}>{aarsak}</List.Item>
            ))}
          </List>
        </span>
      </HelpText>
    );
  }

  return null;
}

function aarsakTilTekst(aarsak: TilsagnTilAnnulleringAarsak): string {
  switch (aarsak) {
    case TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING:
      return "Feilregistrering";
    case TilsagnTilAnnulleringAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES:
      return "Tiltaket skal ikke gjennomføres";
    case TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV:
      return "Arrangør har ikke sendt krav";
    case TilsagnTilAnnulleringAarsak.FEIL_ANNET:
      return "Annen årsak";
  }
}
