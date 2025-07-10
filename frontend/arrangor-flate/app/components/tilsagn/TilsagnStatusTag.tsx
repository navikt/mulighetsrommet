import { TilsagnStatus, TilsagnStatusOgAarsaker, TilsagnTilAnnulleringAarsak } from "api-client";
import { HelpText, HStack, List, Tag } from "@navikt/ds-react";

interface Props {
  data: TilsagnStatusOgAarsaker;
}

export function TilsagnStatusTag({ data }: Props) {
  const aarsaker = data.aarsaker || [];
  switch (data.status) {
    case TilsagnStatus.GODKJENT:
      return <Tag variant="success">Godkjent</Tag>;
    case TilsagnStatus.ANNULLERT:
      return (
        <HStack gap="2" align="center" wrap={false}>
          <Tag variant="error" className="line-through bg-white! text-text-danger!">
            Annullert
          </Tag>
          {skalViseHelptext("Tilsagnet er annullert med følgende årsak(er):", aarsaker)}
        </HStack>
      );
    case TilsagnStatus.TIL_ANNULLERING:
      return (
        <HStack gap="2" align="center" wrap={false}>
          <Tag variant="warning" className="whitespace-nowrap">
            Til annullering
          </Tag>
          {skalViseHelptext(
            "Tilsagnet er til annullering hos NAV med følgende årsak(er):",
            aarsaker,
          )}
        </HStack>
      );
    case TilsagnStatus.TIL_OPPGJOR:
      return <Tag variant="warning">Til oppgjør</Tag>;

    case TilsagnStatus.OPPGJORT:
      return <Tag variant="neutral">Oppgjort</Tag>;

    default:
      return null;
  }
}

function skalViseHelptext(beskrivelse: string, aarsaker: TilsagnTilAnnulleringAarsak[]) {
  if (aarsaker.length > 0) {
    return (
      <HelpText>
        <p>{beskrivelse} </p>
        <List>
          {aarsaker.map(aarsakTilTekst).map((aarsak) => (
            <List.Item key={aarsak}>{aarsak}</List.Item>
          ))}
        </List>
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
