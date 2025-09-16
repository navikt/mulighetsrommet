import { AvtaleStatusDto } from "@mr/api-client-v2";
import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";

interface Props {
  status: AvtaleStatusDto;
}

export function AvtaleStatusTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
