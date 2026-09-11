import { DataElementStatusTag } from "@mr/frontend-common";
import { DataElementStatus } from "@tiltaksadministrasjon/api-client";

interface Props {
  status: DataElementStatus;
}

export function GjennomforingStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.value} variant={status.variant} />;
}
