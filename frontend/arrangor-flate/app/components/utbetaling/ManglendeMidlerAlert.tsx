import { Alert, Heading } from "@navikt/ds-react";
import { ArrangorflateTilsagnDto } from "api-client";

interface ManglendeMidlerAlertProps {
  belopTilUtbetaling: number;
  tilsagn: ArrangorflateTilsagnDto[];
}

export function ManglendeMidlerAlert({ belopTilUtbetaling, tilsagn }: ManglendeMidlerAlertProps) {
  if (tilsagn.length === 0) {
    return null;
  }
  const gjenstaendeTotalt = tilsagn.reduce<number>((acc, { gjenstaendeBelop }) => {
    return acc + gjenstaendeBelop;
  }, 0);
  if (gjenstaendeTotalt >= belopTilUtbetaling) {
    return null;
  }

  return (
    <Alert variant={"warning"}>
      <Heading spacing level="4" size="small">
        Manglende midler
      </Heading>
      Det er ikke nok midler igjen på tilsagnet til å dekke hele utbetalingsbeløpet. Dere kan sende
      inn kravet, men det vil i etterkant behandles av Nav som vil vurdere om hele beløpet skal
      utbetales. Vennligst ta kontakt med Nav ved spørsmål.
    </Alert>
  );
}
