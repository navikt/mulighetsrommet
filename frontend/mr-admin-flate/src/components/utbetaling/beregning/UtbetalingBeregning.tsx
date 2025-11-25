import { DataDrivenTable, getDataElement } from "@mr/frontend-common";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { CopyButton, Heading, HStack, VStack } from "@navikt/ds-react";
import { DataDetails, UtbetalingBeregningDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  beregning: UtbetalingBeregningDto;
}

export default function UtbetalingBeregning({ beregning }: Props) {
  return (
    <VStack gap="2">
      {beregning.deltakerTableData && <DataDrivenTable data={beregning.deltakerTableData} />}
      <SatsPerioderOgBelop satsDetaljer={beregning.satsDetaljer} belop={beregning.belop} />
    </VStack>
  );
}

function SatsPerioderOgBelop({
  belop,
  satsDetaljer,
}: {
  belop: number;
  satsDetaljer: DataDetails[];
}) {
  return (
    <VStack className="max-w-[500px]" gap="2">
      {satsDetaljer.length > 0 && (
        <>
          {satsDetaljer.map((s) => (
            <VStack>
              {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
              {s.entries.map((entry) => (
                <HStack justify="space-between">
                  <dt>{entry.label}:</dt>
                  <dd className="font-bold whitespace-nowrap w-fit">
                    {entry.value ? getDataElement(entry.value) : "-"}
                  </dd>
                </HStack>
              ))}
            </VStack>
          ))}
          <hr className="w-[500px] bg-[var(--a-border-divider)] h-px border-0" />
        </>
      )}
      <HStack justify="space-between">
        <dt>Beløp:</dt>
        <dd className="font-bold whitespace-nowrap w-fit">
          <CopyButton
            variant="action"
            size="small"
            copyText={belop.toString()}
            text={formaterNOK(belop)}
          />
        </dd>
      </HStack>
    </VStack>
  );
}
