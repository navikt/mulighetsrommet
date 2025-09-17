import { TilsagnStatusDto } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";

interface Props {
  status: TilsagnStatusDto;
}

export function TilsagnTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
