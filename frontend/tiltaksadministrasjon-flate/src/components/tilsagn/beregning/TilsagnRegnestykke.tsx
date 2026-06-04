import { CalculationDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { DataDrivenTable, getDataElement } from "@mr/frontend-common";

interface Props {
  regnestykke: CalculationDto;
}

export function TilsagnRegnestykke({ regnestykke }: Props) {
  const expression = regnestykke.expression ? (
    <HStack gap="space-8">
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
