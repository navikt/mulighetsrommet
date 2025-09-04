import { DelutbetalingStatusDto } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";

interface Props {
  status: DelutbetalingStatusDto;
}

export function DelutbetalingStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
