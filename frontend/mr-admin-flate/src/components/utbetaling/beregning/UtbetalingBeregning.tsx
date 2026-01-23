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
    <VStack gap="2">
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
    <VStack className="max-w-[500px]" gap="2">
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
          <hr className="w-[500px] bg-[var(--a-border-divider)] h-px border-0" />
        </>
      )}
      <MetadataHStack
        label="Innsendt beløp"
        value={
          <CopyButton
            variant="action"
            size="small"
            copyText={pris.belop.toString()}
            text={formaterValutaBelop(pris)}
          />
        }
      />
    </VStack>
  );
}
