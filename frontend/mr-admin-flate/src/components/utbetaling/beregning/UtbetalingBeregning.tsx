import { DataDrivenTable, getDataElement } from "@mr/frontend-common";
import { MetadataHStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { CopyButton, Heading, VStack } from "@navikt/ds-react";
import {
  DataDetails,
  UtbetalingBeregningDto,
  ValutaBelop,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  beregning: UtbetalingBeregningDto;
}

export default function UtbetalingBeregning({ beregning }: Props) {
  return (
    <VStack gap="space-8">
      {beregning.deltakerTableData && <DataDrivenTable data={beregning.deltakerTableData} />}
      <SatsPerioderOgBelop satsDetaljer={beregning.satsDetaljer} pris={beregning.pris} />
    </VStack>
  );
}

function SatsPerioderOgBelop({
  pris,
  satsDetaljer,
}: {
  pris: ValutaBelop;
  satsDetaljer: DataDetails[];
}) {
  return (
    <VStack className="max-w-[500px]" gap="space-8">
      {satsDetaljer.length > 0 && (
        <>
          {satsDetaljer.map((s) => (
            <VStack>
              {satsDetaljer.length > 1 && <Heading size="xsmall">{s.header}</Heading>}
              {s.entries.map((entry) => (
                <MetadataHStack
                  label={entry.label}
                  value={entry.value ? getDataElement(entry.value) : null}
                />
              ))}
            </VStack>
          ))}
          <hr className="w-[500px] bg-[var(--ax-border-neutral-subtle)] h-px border-0" />
        </>
      )}
      <MetadataHStack
        label="Innsendt beløp"
        value={
          <CopyButton
            size="small"
            copyText={pris.belop.toString()}
            text={formaterValutaBelop(pris)}
          />
        }
      />
    </VStack>
  );
}
