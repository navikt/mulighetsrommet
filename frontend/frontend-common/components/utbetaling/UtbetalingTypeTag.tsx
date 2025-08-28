import { UtbetalingType } from "@api-client";
import { HStack, Tag } from "@navikt/ds-react";

interface UtbetalingsTypeTagProps {
  type: UtbetalingType;
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
  type: UtbetalingType;
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
