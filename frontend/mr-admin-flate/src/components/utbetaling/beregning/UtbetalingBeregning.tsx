import { CopyButton, HStack, VStack } from "@navikt/ds-react";
import {
  DataDrivenTableDto,
  DataElement,
  UtbetalingBeregningDto,
} from "@tiltaksadministrasjon/api-client";
import { DataDrivenTable } from "@/components/tabell/DataDrivenTable";
import { getDataElement } from "@/components/data-element/DataElement";

interface Props {
  beregning: UtbetalingBeregningDto;
}

export default function UtbetalingBeregning({ beregning }: Props) {
  return (
    <VStack gap="2">
      {beregning.deltakerTableData.rows.length > 0 && (
        <DataDrivenTable data={beregning.deltakerTableData as unknown as DataDrivenTableDto} />
      )}
      <UtbetalingRegnestykke {...beregning} />
    </VStack>
  );
}

function UtbetalingRegnestykke({ regnestykke }: UtbetalingBeregningDto) {
  const expression = regnestykke.slice(0, -1);
  const result = regnestykke[regnestykke.length - 1];
  return (
    <HStack gap="2">
      {expression.map((entry, idx) => (
        <span key={idx} className="font-bold">
          {getDataElement(entry)}
        </span>
      ))}
      <UtbetalingRegnestykkeResult {...result} />
    </HStack>
  );
}

function UtbetalingRegnestykkeResult(element: DataElement) {
  const renderedElement = getDataElement(element);

  if (
    element.type === "no.nav.mulighetsrommet.model.DataElement.Text" &&
    typeof element.value === "string" &&
    typeof renderedElement === "string"
  ) {
    return (
      <CopyButton variant="action" size="small" copyText={element.value} text={renderedElement} />
    );
  }

  return renderedElement;
}
