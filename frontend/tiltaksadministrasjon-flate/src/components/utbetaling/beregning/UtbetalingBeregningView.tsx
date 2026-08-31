import { Accordion, Button, HStack, InfoCard, List, VStack } from "@navikt/ds-react";
import {
  DeltakerAdvarselDto,
  UtbetalingBeregningDto,
  UtbetalingBlokkering,
} from "@tiltaksadministrasjon/api-client";
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
      {blokkeringer.map((blokkering) => {
        const { title, content } = getBlokkeringTekst(blokkering, beregning.advarsler);
        return (
          <InfoCard data-color="warning" key={blokkering.toString()}>
            <InfoCard.Header>
              <InfoCard.Title>{title}</InfoCard.Title>
            </InfoCard.Header>
            <InfoCard.Content>{content}</InfoCard.Content>
          </InfoCard>
        );
      })}
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

function getBlokkeringTekst(
  blokkering: UtbetalingBlokkering,
  advarsler: DeltakerAdvarselDto[],
): { title: string; content: React.ReactNode } {
  switch (blokkering) {
    case UtbetalingBlokkering.MANGLER_TILSAGN:
      return {
        title: "Tilsagn mangler",
        content: "Det finnes ingen godkjente tilsagn tilgjengelig for denne utbetalingen.",
      };
    case UtbetalingBlokkering.UBEHANDLET_FORSLAG:
      return {
        title: "Viktig informasjon om deltakere",
        content: (
          <>
            Det finnes advarsler i Deltakeroversikten for følgende personer:
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
