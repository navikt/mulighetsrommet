import { DataElementStatusTag } from "@mr/frontend-common";
import { TilsagnStatusDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  status: TilsagnStatusDto;
}

export function TilsagnTag({ status }: Props) {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
