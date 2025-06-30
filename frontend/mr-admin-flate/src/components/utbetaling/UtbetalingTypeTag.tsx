import { UtbetalingType } from "@mr/api-client-v2";
import { HStack, Tag } from "@navikt/ds-react";

interface UtbetalingsTypeTagProps {
  type: UtbetalingType;
}

export function UtbetalingTypeTag({ type }: UtbetalingsTypeTagProps) {
  return (
    <Tag variant="neutral" title={visningsNavn(type)} size="small">
      {tagNavn(type)}
    </Tag>
  );
}

interface UtbetalingsTypeProps {
  type: UtbetalingType;
}

export function UtbetalingTypeText({ type }: UtbetalingsTypeProps) {
  return (
    <HStack gap="2">
      {visningsNavn(type)}
      <UtbetalingTypeTag type={type} />
    </HStack>
  );
}

function visningsNavn(type: UtbetalingType) {
  switch (type) {
    case UtbetalingType.KORRIGERING:
      return "Korrigering";
    case UtbetalingType.INVESTERING:
      return "Investering";
  }
}

function tagNavn(type: UtbetalingType) {
  switch (type) {
    case UtbetalingType.KORRIGERING:
      return "KOR";
    case UtbetalingType.INVESTERING:
      return "INV";
  }
}
