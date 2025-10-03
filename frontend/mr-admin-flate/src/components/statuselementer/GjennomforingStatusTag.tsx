import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";
import { GjennomforingDtoStatus } from "@tiltaksadministrasjon/api-client";

interface Props {
  status: GjennomforingDtoStatus;
}

export function GjennomforingStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
