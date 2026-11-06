import {
  DataDetails as DataDetailsDto,
  UtbetalingsdetaljerDto,
} from "@arrangor-utbetalinger/api-client";
import { DataDetails } from "@mr/frontend-common";
import { Box, Heading } from "@navikt/ds-react";

export function UtbetalingsdetaljerSection({ detaljer }: { detaljer: DataDetailsDto }) {
  return (
    <>
      {detaljer.header && (
        <Heading level="3" size="medium">
          {detaljer.header}
        </Heading>
      )}
      <DataDetails entries={detaljer.entries} />
    </>
  );
}

export function UtbetalingsdetaljerBeregning({
  detaljer,
}: {
  detaljer: Pick<UtbetalingsdetaljerDto, "utbetaling" | "satsDetaljer">;
}) {
  return (
    <Box width="50%">
      <Heading level="3" size="medium">
        {detaljer.utbetaling.header}
      </Heading>
      {detaljer.satsDetaljer.map((sats, index) => (
        <Box key={index} marginBlock="space-8 space-0">
          {sats.header && (
            <Heading level="4" size="xsmall">
              {sats.header}
            </Heading>
          )}
          <DataDetails entries={sats.entries} />
        </Box>
      ))}
      <DataDetails entries={detaljer.utbetaling.entries} />
    </Box>
  );
}
