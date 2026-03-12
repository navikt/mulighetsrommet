import { Accordion, BodyShort, Button, HStack, InfoCard, List, VStack } from "@navikt/ds-react";
import { UtbetalingBeregningDto } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";
import UtbetalingBeregning from "./UtbetalingBeregning";
import { UtbetalingBeregningModal } from "./UtbetalingBeregningModal";

interface Props {
  beregning: UtbetalingBeregningDto;
  utbetalingId: string;
}

export default function UtbetalingBeregningView({ beregning, utbetalingId }: Props) {
  const [beregningModalOpen, setBeregningModalOpen] = useState<boolean>(false);

  return (
    <>
      {beregning.advarsler.length > 0 && (
        <InfoCard data-color="warning">
          <InfoCard.Header>
            <InfoCard.Title>Viktig informasjon om deltakere</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            <BodyShort spacing>
              Det finnes advarsler på følgende personer. Disse må først fikses før utbetalingen kan
              sendes inn.
            </BodyShort>
            <List>
              {beregning.advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>{advarsel.beskrivelse}</List.Item>
              ))}
            </List>
          </InfoCard.Content>
        </InfoCard>
      )}
      <Accordion>
        <Accordion.Item>
          <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
          <Accordion.Content>
            <VStack gap="space-8">
              <UtbetalingBeregning beregning={beregning} />
              <HStack justify="start" align="start">
                {beregning.deltakerTableData && (
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
    </>
  );
}
