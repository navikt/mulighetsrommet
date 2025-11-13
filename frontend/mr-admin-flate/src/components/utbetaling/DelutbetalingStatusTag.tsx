import { DataElementStatusTag } from "@mr/frontend-common";
import { DelutbetalingStatusDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  status: DelutbetalingStatusDto;
}

export function DelutbetalingStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
