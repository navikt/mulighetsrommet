import { Tag } from "@navikt/ds-react";
import { TiltaksgjennomforingStatusDto } from "@mr/api-client";
import { useState } from "react";
import { variantAndName } from "./GjennomforingStatusTag";

interface Props {
  status: TiltaksgjennomforingStatusDto;
}

export function GjennomforingStatusMedAarsakTag({ status }: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const { variant, label } = variantAndName(status.status);
  const labelWithBeskrivelse = status.avbrutt?.beskrivelse
    ? `${label} - ${status.avbrutt.beskrivelse}`
    : label;

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
