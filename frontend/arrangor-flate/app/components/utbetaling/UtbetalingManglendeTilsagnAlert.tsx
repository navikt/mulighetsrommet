import { BodyShort, InfoCard, VStack } from "@navikt/ds-react";

export function UtbetalingManglendeTilsagnAlert() {
  return (
    <VStack align="start">
      <InfoCard data-color="warning" size="small">
        <InfoCard.Header>
          <InfoCard.Title as="h4">Tilsagn mangler</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          <BodyShort>
            Det finnes ingen godkjente tilsagn tilgjengelig for denne utbetalingen.
          </BodyShort>
          <BodyShort spacing>
            Dere kan ikke sende inn kravet før Nav har godkjent et tilsagn for utbetalingsperioden.
          </BodyShort>
          Vennligst ta kontakt med Nav.
        </InfoCard.Content>
      </InfoCard>
    </VStack>
  );
}
