import { HStack, Tag } from "@navikt/ds-react";

enum UtbetalingType {
    KORRIGERING = 'KORRIGERING',
    INVESTERING = 'INVESTERING'
}

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
  text?: String
  type: UtbetalingType;
}

export function UtbetalingTypeText({ text, type }: UtbetalingsTypeProps) {
  return (
    <HStack gap="2">
      {text || visningsNavn(type)}
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
