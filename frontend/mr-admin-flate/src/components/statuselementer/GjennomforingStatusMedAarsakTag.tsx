import { Tag } from "@navikt/ds-react";
import { useState } from "react";
import { variantAndName } from "@mr/frontend-common/components/gjennomforing/GjennomforingStatusTag";
import { GjennomforingStatusDto } from "@mr/api-client-v2";

interface Props {
  status: GjennomforingStatusDto;
}

export function GjennomforingStatusMedAarsakTag({ status }: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const { variant, label } = variantAndName(status.type);
  const labelWithBeskrivelse = "beskrivelse" in status ? `${label} - ${status.beskrivelse}` : label;

  return (
    <Tag
      style={{
        maxWidth: "400px",
      }}
      size="small"
      onMouseEnter={() => setExpandLabel(true)}
      onMouseLeave={() => setExpandLabel(false)}
      aria-label={`Gjennomføringstatus: ${label}`}
      variant={variant}
    >
      {expandLabel ? labelWithBeskrivelse : truncate(labelWithBeskrivelse, 30)}
    </Tag>
  );
}

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
