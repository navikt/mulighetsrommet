import { Box, Heading } from "@navikt/ds-react";
import { DataDetails, ValutaBelop } from "@api-client";
import { getDataElement } from "@mr/frontend-common";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { MetadataHGrid, Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function SatsPerioderOgBelop({
  pris,
  satsDetaljer,
}: {
  pris: ValutaBelop;
  satsDetaljer: DataDetails[];
}) {
  return (
    <Box width="50%">
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
      <Separator />
      <MetadataHGrid label="Beløp" value={formaterValutaBelop(pris)} />
    </Box>
  );
}
