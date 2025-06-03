import { AvtaleStatusDto } from "@mr/api-client-v2";
import { StatusTag } from "@mr/frontend-common";
import { getAvtaleStatusTagProps } from "@/components/statuselementer/getStatusTagProps";

interface Props {
  status: AvtaleStatusDto;
}

export function AvtaleStatusTag({ status }: Props) {
  const { variant, name } = getAvtaleStatusTagProps(status.type);
  return (
    <StatusTag aria-label={`Status for avtale: ${name}`} variant={variant}>
      {name}
    </StatusTag>
  );
}
