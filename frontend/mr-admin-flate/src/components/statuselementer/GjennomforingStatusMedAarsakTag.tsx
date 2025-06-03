import { GjennomforingStatusDto } from "@mr/api-client-v2";
import { ExpandableStatusTag } from "@mr/frontend-common";
import { getGjennomforingStatusTagsProps } from "./getStatusTagProps";

interface Props {
  status: GjennomforingStatusDto;
}

export function GjennomforingStatusMedAarsakTag({ status }: Props) {
  const { variant, name } = getGjennomforingStatusTagsProps(status.type);
  const label = "beskrivelse" in status ? `${name} - ${status.beskrivelse}` : name;
  return (
    <ExpandableStatusTag aria-label={`Status for gjennomføring: ${name}`} variant={variant}>
      {label}
    </ExpandableStatusTag>
  );
}
