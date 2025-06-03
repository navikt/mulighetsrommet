import { AvtaleStatusDto } from "@mr/api-client-v2";
import { Tag } from "@navikt/ds-react";
import { useState } from "react";
import { getVariantAndName } from "@/components/statuselementer/AvtaleStatusTag";

interface Props {
  status: AvtaleStatusDto;
}

export function AvtaleStatusMedAarsakTag({ status }: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const { variant, name } = getVariantAndName(status.type);
  const label = "beskrivelse" in status ? `${name} - ${status.beskrivelse}` : name;

  return (
    <Tag
      className="min-w-[140px] text-center whitespace-nowrap"
      size="small"
      onMouseEnter={() => setExpandLabel(true)}
      onMouseLeave={() => setExpandLabel(false)}
      aria-label={`Avtalestatus: ${name}`}
      variant={variant}
    >
      {expandLabel ? label : truncate(label, 30)}
    </Tag>
  );
}

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
