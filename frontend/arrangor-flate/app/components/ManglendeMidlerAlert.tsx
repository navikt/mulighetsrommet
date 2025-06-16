import { Alert, Heading } from "@navikt/ds-react";

export function ManglendeMidlerAlert() {
  return (
    <Alert variant={"warning"}>
      <Heading spacing level="4" size="small">
        Manglende midler
      </Heading>
      Det mangler midler på tilsagnet til å dekke hele utbetalingsbeløpet. Nav vil vurdere hva som
      utbetales. Vennligst ta kontakt med Nav ved spørsmål.
    </Alert>
  );
}
