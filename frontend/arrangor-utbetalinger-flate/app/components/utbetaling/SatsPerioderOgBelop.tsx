import { Box, Heading } from "@navikt/ds-react";
import { ArrangorflatePris, DataDetails } from "@arrangor-utbetalinger/api-client";
import { getDataElement } from "@mr/frontend-common";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterArrangorflatePris } from "~/utils/utbetaling";

interface SatsPerioderOgBelopProps {
  pris: ArrangorflatePris;
  satsDetaljer: DataDetails[];
}

export function SatsPerioderOgBelop({ pris, satsDetaljer }: SatsPerioderOgBelopProps) {
  return (
    <Box width="50%">
      <Heading level="3" size="medium">
        Utbetaling
      </Heading>
      {satsDetaljer.map((s) => (
        <Box key={s.header} marginBlock="space-8 space-0">
          {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
          {s.entries.map((entry) => (
            <MetadataHGrid
              label={entry.label}
              value={entry.value ? getDataElement(entry.value) : null}
            />
          ))}
        </Box>
      ))}
      <MetadataHGrid label="Beløp" value={formaterArrangorflatePris(pris)} />
    </Box>
  );
}
