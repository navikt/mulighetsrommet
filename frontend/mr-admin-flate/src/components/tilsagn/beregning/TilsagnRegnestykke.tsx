import { CalculationDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { DataDrivenTable } from "@/components/tabell/DataDrivenTable";
import { getDataElement } from "@/components/data-element/DataElement";

interface Props {
  regnestykke: CalculationDto;
}

export function TilsagnRegnestykke({ regnestykke }: Props) {
  const expression = regnestykke.expression ? (
    <HStack gap="2">
      {regnestykke.expression.map((entry, idx) => (
        <span key={idx}>{getDataElement(entry)} </span>
      ))}
    </HStack>
  ) : null;

  const breakdown = regnestykke.breakdown ? <DataDrivenTable data={regnestykke.breakdown} /> : null;

  return expression || breakdown ? (
    <>
      {expression}
      {breakdown}
    </>
  ) : null;
}
