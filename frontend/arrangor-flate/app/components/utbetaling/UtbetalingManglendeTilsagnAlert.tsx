import { Alert, Heading } from "@navikt/ds-react";

export function UtbetalingManglendeTilsagnAlert() {
  return (
    <Alert variant={"warning"}>
      <Heading spacing level="4" size="small">
        Tilsagn mangler
      </Heading>
      Det finnes ingen godkjente tilsagn tilgjengelig for denne utbetalingen. Dere kan ikke sende
      inn kravet før Nav har godkjent et tilsagn for utbetalingsperioden. Vennligst ta kontakt med
      Nav.
    </Alert>
  );
}
