import { GjennomforingStatusDto } from "@mr/api-client-v2";
import { StatusTag } from "@mr/frontend-common";
import { getGjennomforingStatusTagsProps } from "@/components/statuselementer/getStatusTagProps";

interface Props {
  status: GjennomforingStatusDto;
}

export function GjennomforingStatusTag({ status }: Props) {
  const { variant, name } = getGjennomforingStatusTagsProps(status.type);
  return (
    <StatusTag variant={variant} aria-label={`Status for gjennomføring: ${name}`}>
      {name}
    </StatusTag>
  );
}
