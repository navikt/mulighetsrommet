import { Accordion, Heading, VStack } from "@navikt/ds-react";
import { UtbetalingsdetaljerDto } from "@arrangor-utbetalinger/api-client";
import { DataDetails } from "@mr/frontend-common";
import { UtbetalingsdetaljerSection } from "./Utbetalingsdetaljer";

interface Props {
  detaljer: UtbetalingsdetaljerDto;
}

export default function UtbetalingStatusList({ detaljer }: Props) {
  return (
    <VStack gap="space-16">
      <UtbetalingsdetaljerSection detaljer={detaljer.status} />
      {detaljer.avbrytelse && <UtbetalingsdetaljerSection detaljer={detaljer.avbrytelse} />}
      {detaljer.tilsagn.length > 0 && (
        <>
          <Heading size="small" level="4">
            {detaljer.tilsagnHeader}
          </Heading>
          <Accordion>
            {detaljer.tilsagn.map((linje, index) => (
              <Accordion.Item key={index}>
                <Accordion.Header>{linje.header}</Accordion.Header>
                <Accordion.Content>
                  <DataDetails entries={linje.entries} />
                </Accordion.Content>
              </Accordion.Item>
            ))}
          </Accordion>
        </>
      )}
    </VStack>
  );
}
