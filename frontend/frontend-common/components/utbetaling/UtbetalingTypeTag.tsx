import { Tag } from "@navikt/ds-react";

interface UtbetalingsTypeTagProps {
  type: "Investering" | "Korrigering" | string;
}

export function UtbetalingTypeTag({ type }: UtbetalingsTypeTagProps) {
  if (type !== "Investering" && type !== "Korrigering") {
    return null;
  }
  return (
    <Tag variant="neutral" title={type} size="small">
      {type === "Investering" ? "INV" : "KOR"}
    </Tag>
  );
}
