import { CalculationDto } from "@mr/api-client-v2";
import { HStack } from "@navikt/ds-react";
import { DataDrivenTable, renderCell } from "@/components/tabell/DataDrivenTable";

interface Props {
  regnestykke: CalculationDto;
}

export function TilsagnRegnestykke({ regnestykke }: Props) {
  const expression = regnestykke.expression ? (
    <HStack gap="2">
      {regnestykke.expression.map((entry, idx) => (
        <span key={idx}>{renderCell(entry)} </span>
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
