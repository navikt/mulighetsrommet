import { AvtaleStatusDto } from "@mr/api-client-v2";
import { ExpandableStatusTag } from "@mr/frontend-common";
import { getAvtaleStatusTagProps } from "@/components/statuselementer/getStatusTagProps";

interface Props {
  status: AvtaleStatusDto;
}

export function AvtaleStatusMedAarsakTag({ status }: Props) {
  const { variant, name } = getAvtaleStatusTagProps(status.type);
  const label = "beskrivelse" in status ? `${name} - ${status.beskrivelse}` : name;
  return (
    <ExpandableStatusTag aria-label={`Status for avtale: ${name}`} variant={variant}>
      {label}
    </ExpandableStatusTag>
  );
}
