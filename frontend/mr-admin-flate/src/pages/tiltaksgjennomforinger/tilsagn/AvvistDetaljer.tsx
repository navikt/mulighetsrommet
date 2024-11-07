import { TilsagnAvvisningAarsak, TilsagnBesluttelseStatus, TilsagnDto } from "@mr/api-client";
import { Alert, Heading, HGrid, List } from "@navikt/ds-react";
import { Metadata } from "../../../components/detaljside/Metadata";
import { DetaljerInfoContainer } from "../../DetaljerInfoContainer";

interface Props {
  tilsagn: TilsagnDto;
}

export function AvvistDetaljer({ tilsagn }: Props) {
  const { besluttelse } = tilsagn;

  return besluttelse?.status === TilsagnBesluttelseStatus.AVVIST && besluttelse?.aarsaker ? (
    <DetaljerInfoContainer withBorderRight={false}>
      <Alert variant="warning">
        <Heading size="xsmall" level="3">
          Tilsagnet er ikke godkjent
        </Heading>
        <p>Du må fikse følgende før tilsagnet kan godkjennes:</p>
        <HGrid columns={2} style={{ marginTop: "1rem" }}>
          <Metadata
            header={besluttelse?.aarsaker?.length === 1 ? "Årsak" : "Årsaker"}
            verdi={
              <List>
                {besluttelse?.aarsaker?.map((aarsak, index) => (
                  <List.Item key={index}>{tilsagnAarsakTilTekst(aarsak)}</List.Item>
                ))}
              </List>
            }
          />
          {besluttelse?.forklaring ? (
            <Metadata header="Forklaring" verdi={besluttelse?.forklaring} />
          ) : null}
        </HGrid>
      </Alert>
    </DetaljerInfoContainer>
  ) : null;
}
type TilsagnAarsak =
  | "Feil periode"
  | "Feil antall plasser"
  | "Feil kostnadssted"
  | "Feil beløp"
  | "Annet - Se forklaring";

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
      return "Annet - Se forklaring";
  }
}
