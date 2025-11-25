import { Heading, VStack } from "@navikt/ds-react";
import { DataDetails } from "@api-client";
import { getDataElement } from "@mr/frontend-common";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
import { Separator } from "../common/Separator";

export function SatsPerioderOgBelop({
  belop,
  satsDetaljer,
}: {
  belop: number;
  satsDetaljer: DataDetails[];
}) {
  return (
    <VStack className="w-[700px]" gap="2">
      {satsDetaljer.length > 0 && (
        <>
          {satsDetaljer.map((s) => (
            <VStack>
              {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
              {s.entries.map((entry) => (
                <MetadataHGrid
                  label={entry.label}
                  value={entry.value ? getDataElement(entry.value) : null}
                />
              ))}
            </VStack>
          ))}
          <Separator className="w-[350px]" />
        </>
      )}
      <MetadataHGrid label="Beløp" value={formaterNOK(belop)} />
    </VStack>
  );
}
