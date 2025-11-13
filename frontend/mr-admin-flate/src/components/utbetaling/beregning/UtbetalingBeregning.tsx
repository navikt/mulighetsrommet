import { DataDrivenTable, getDataElement } from "@mr/frontend-common";
import { CopyButton, HStack, VStack } from "@navikt/ds-react";
import { DataElement, UtbetalingBeregningDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  beregning: UtbetalingBeregningDto;
}

export default function UtbetalingBeregning({ beregning }: Props) {
  return (
    <VStack gap="2">
      {beregning.deltakerTableData && <DataDrivenTable data={beregning.deltakerTableData} />}
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
