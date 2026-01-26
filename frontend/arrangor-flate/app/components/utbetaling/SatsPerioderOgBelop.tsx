import { Heading, VStack } from "@navikt/ds-react";
import { DataDetails, ValutaBelop } from "@api-client";
import { getDataElement } from "@mr/frontend-common";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
import { Separator } from "../common/Separator";

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
    <VStack className={className} gap="2">
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
          <Separator className="w-1/2" />
        </>
      )}
      <MetadataHGrid label="Beløp" value={formaterValutaBelop(pris)} />
    </VStack>
  );
}
