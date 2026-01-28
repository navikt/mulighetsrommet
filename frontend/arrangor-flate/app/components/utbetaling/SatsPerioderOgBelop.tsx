import { Heading, VStack } from "@navikt/ds-react";
import { DataDetails, ValutaBelop } from "@api-client";
import { getDataElement } from "@mr/frontend-common";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { MetadataHGrid, Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function SatsPerioderOgBelop({
  pris,
  satsDetaljer,
  className,
}: {
  pris: ValutaBelop;
  satsDetaljer: DataDetails[];
  className?: string;
}) {
  return (
    <VStack className={className} gap="2" width="50%">
      {satsDetaljer.map((s) => (
        <>
          {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
          {s.entries.map((entry) => (
            <MetadataHGrid
              label={entry.label}
              value={entry.value ? getDataElement(entry.value) : null}
            />
          ))}
        </>
      ))}
      <Separator />
      <MetadataHGrid label="Beløp" value={formaterValutaBelop(pris)} />
    </VStack>
  );
}
