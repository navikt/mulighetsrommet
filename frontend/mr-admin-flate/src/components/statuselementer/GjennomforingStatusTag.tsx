import { GjennomforingDtoStatus } from "@mr/api-client-v2";
import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";

interface Props {
  status: GjennomforingDtoStatus;
}

export function GjennomforingStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
