import { DeltakerAdvarselDto, UtbetalingBlokkering } from "@arrangor-utbetalinger/api-client";
import { VStack, InfoCard, List, Heading, InlineMessage, Box } from "@navikt/ds-react";

export function BlokkeringerVarsler({
  blokkeringer,
  advarsler,
}: {
  blokkeringer: UtbetalingBlokkering[];
  advarsler: DeltakerAdvarselDto[];
}) {
  if (blokkeringer.length === 0) {
    return null;
  }
  return (
    <Box>
      <Heading level="3" size="medium" spacing>
        Nav må rette opp følgende før kravet kan sendes inn
      </Heading>
      <VStack gap="space-16">
        <InlineMessage status="info">
          Vennligst ta kontakt med Nav dersom problemene vedvarer.
        </InlineMessage>
        {blokkeringer.includes(UtbetalingBlokkering.MANGLER_TILSAGN) && (
          <InfoCard data-color="warning">
            <InfoCard.Header>
              <InfoCard.Title>Tilsagn mangler</InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              Det finnes ingen godkjente tilsagn for dette kravet. Dere kan ikke sende inn kravet
              før Nav har godkjent et tilsagn for utbetalingsperioden.
            </InfoCard.Content>
          </InfoCard>
        )}
        {blokkeringer.includes(UtbetalingBlokkering.UBEHANDLET_FORSLAG) && (
          <InfoCard data-color="warning">
            <InfoCard.Header>
              <InfoCard.Title>Viktig informasjon om deltakere</InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>
              Det finnes advarsler i Deltakeroversikten for følgende personer. Nav veileder må
              behandle disse før kravet kan sendes inn.
              <List>
                {advarsler.map((advarsel) => (
                  <List.Item key={advarsel.deltakerId}>
                    <b>{advarsel.navn}</b> {advarsel.beskrivelse}
                  </List.Item>
                ))}
              </List>
            </InfoCard.Content>
          </InfoCard>
        )}
      </VStack>
    </Box>
  );
}
