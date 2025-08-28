import { HStack, Tag } from "@navikt/ds-react";

export type UtbetalingTypeDto = {
    displayName: string;
    displayNameLong: string | null;
    tagName: string | null;
};

interface UtbetalingsTypeTagProps {
  type: UtbetalingTypeDto;
}

export function UtbetalingTypeTag({ type }: UtbetalingsTypeTagProps) {
  if (!type.tagName) {
    return null
  }
  return (
    <Tag variant="neutral" title={type.displayName} size="small">
      {type.tagName}
    </Tag>
  );
}

interface UtbetalingsTypeProps {
  type: UtbetalingTypeDto;
}

export function UtbetalingTypeText({ type }: UtbetalingsTypeProps) {
  if (!type.tagName){
    return null;
  }
  return (
    <HStack gap="2">
      {type.displayName}
      <UtbetalingTypeTag type={type} />
    </HStack>
  );
}
