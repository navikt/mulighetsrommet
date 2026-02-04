import { BodyShort, LocalAlert, VStack } from "@navikt/ds-react";

export function UtbetalingManglendeTilsagnAlert() {
  return (
    <VStack align="start">
      <LocalAlert status="warning" size="small">
        <LocalAlert.Header>
          <LocalAlert.Title as="h4">Tilsagn mangler</LocalAlert.Title>
        </LocalAlert.Header>
        <LocalAlert.Content>
          <BodyShort>
            Det finnes ingen godkjente tilsagn tilgjengelig for denne utbetalingen.
          </BodyShort>
          <BodyShort spacing>
            Dere kan ikke sende inn kravet før Nav har godkjent et tilsagn for utbetalingsperioden.
          </BodyShort>
          Vennligst ta kontakt med Nav.
        </LocalAlert.Content>
      </LocalAlert>
    </VStack>
  );
}
