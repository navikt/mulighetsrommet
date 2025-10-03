import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";
import { AvtaleDtoStatus } from "@tiltaksadministrasjon/api-client";

interface Props {
  status: AvtaleDtoStatus;
}

export function AvtaleStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
