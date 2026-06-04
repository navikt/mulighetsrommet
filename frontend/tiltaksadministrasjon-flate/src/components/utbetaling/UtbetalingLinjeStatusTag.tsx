import { DataElementStatusTag } from "@mr/frontend-common";
import { UtbetalingLinjeStatusDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  status: UtbetalingLinjeStatusDto;
}

export function UtbetalingLinjeStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
