import { Heading, HStack, VStack } from "@navikt/ds-react";
import { DataDetails } from "@api-client";
import { getDataElement } from "@mr/frontend-common";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

export function SatsPerioderOgBelop({
  belop,
  satsDetaljer,
}: {
  belop: number;
  satsDetaljer: DataDetails[];
}) {
  return (
    <VStack className="w-[400px]" gap="2">
      {satsDetaljer.length > 0 && (
        <>
          {satsDetaljer.map((s) => (
            <VStack>
              {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
              {s.entries.map((entry) => (
                <HStack as="dl" justify="space-between">
                  <dt className="font-bold w-max">{entry.label}:</dt>
                  <dd className="whitespace-nowrap w-fit">
                    {entry.value ? getDataElement(entry.value) : "-"}
                  </dd>
                </HStack>
              ))}
            </VStack>
          ))}
          <hr className="border-t border-border-divider w-full" />
        </>
      )}
      <HStack as="dl" justify="space-between">
        <dt className="font-bold w-max">Beløp:</dt>
        <dd className="whitespace-nowrap w-fit">{formaterNOK(belop)}</dd>
      </HStack>
    </VStack>
  );
}
