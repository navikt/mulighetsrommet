import { Accordion, BodyShort, Button, HStack, InfoCard, List, VStack } from "@navikt/ds-react";
import { UtbetalingBeregningDto, UtbetalingBlokkering } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";
import UtbetalingBeregning from "./UtbetalingBeregning";
import { UtbetalingBeregningModal } from "./UtbetalingBeregningModal";

interface Props {
  beregning: UtbetalingBeregningDto;
  utbetalingId: string;
  blokkeringer: UtbetalingBlokkering[];
}

export default function UtbetalingBeregningView({ beregning, utbetalingId, blokkeringer }: Props) {
  const [beregningModalOpen, setBeregningModalOpen] = useState<boolean>(false);

  return (
    <VStack gap="space-16">
      {beregning.advarsler.length > 0 && (
        <InfoCard data-color="warning">
          <InfoCard.Header>
            <InfoCard.Title>Viktig informasjon om deltakere</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            <BodyShort spacing>
              Det finnes advarsler i Deltakeroversikten for følgende personer.
            </BodyShort>
            <List>
              {beregning.advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>
                  <b>{advarsel.navn}</b> {advarsel.beskrivelse}
                </List.Item>
              ))}
            </List>
          </InfoCard.Content>
        </InfoCard>
      )}
      {blokkeringer.includes(UtbetalingBlokkering.MANGLER_TILSAGN) && (
        <InfoCard data-color="warning">
          <InfoCard.Header>
            <InfoCard.Title>Tilsagn mangler</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            Det finnes ingen godkjente tilsagn tilgjengelig for denne utbetalingen.
          </InfoCard.Content>
        </InfoCard>
      )}
      <Accordion>
        <Accordion.Item>
          <Accordion.Header>{beregning.heading}</Accordion.Header>
          <Accordion.Content>
            <VStack gap="space-8">
              <UtbetalingBeregning beregning={beregning} />
              <HStack justify="start" align="start">
                {beregning.deltakere.length > 0 && (
                  <Button
                    variant="secondary"
                    size="small"
                    onClick={() => setBeregningModalOpen(true)}
                  >
                    Filtreringshjelp
                  </Button>
                )}
              </HStack>
              <UtbetalingBeregningModal
                utbetalingId={utbetalingId}
                modalOpen={beregningModalOpen}
                onClose={() => setBeregningModalOpen(false)}
              />
            </VStack>
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </VStack>
  );
}
