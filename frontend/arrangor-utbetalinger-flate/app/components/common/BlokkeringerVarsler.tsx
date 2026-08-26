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
        {blokkeringer.map((blokkering) => {
          const { title, content } = getBlokkeringTekst(blokkering, advarsler);
          return (
            <InfoCard data-color="warning" key={blokkering.toString()}>
              <InfoCard.Header>
                <InfoCard.Title>{title}</InfoCard.Title>
              </InfoCard.Header>
              <InfoCard.Content>{content}</InfoCard.Content>
            </InfoCard>
          );
        })}
      </VStack>
    </Box>
  );
}

function getBlokkeringTekst(
  blokkering: UtbetalingBlokkering,
  advarsler: DeltakerAdvarselDto[],
): { title: string; content: React.ReactNode } {
  switch (blokkering) {
    case UtbetalingBlokkering.MANGLER_TILSAGN:
      return {
        title: "Tilsagn mangler",
        content:
          "Det finnes ingen godkjente tilsagn for dette kravet. Dere kan ikke sende inn kravet før Nav har godkjent et tilsagn for utbetalingsperioden.",
      };
    case UtbetalingBlokkering.UBEHANDLET_FORSLAG:
      return {
        title: "Viktig informasjon om deltakere",
        content: (
          <>
            Det finnes advarsler i Deltakeroversikten for følgende personer. Nav-veileder må
            behandle disse før kravet kan sendes inn.
            <List>
              {advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>
                  <b>
                    {advarsel.navn}, {advarsel.norskIdent}
                  </b>{" "}
                  {advarsel.beskrivelse}
                </List.Item>
              ))}
            </List>
          </>
        ),
      };
  }
}
