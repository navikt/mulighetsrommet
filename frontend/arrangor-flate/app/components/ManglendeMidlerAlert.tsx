import { Alert, Heading } from "@navikt/ds-react";

export function ManglendeMidlerAlert() {
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
