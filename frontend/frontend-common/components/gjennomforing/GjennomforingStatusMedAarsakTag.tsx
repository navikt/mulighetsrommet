import { Tag } from "@navikt/ds-react";
import { useState } from "react";
import { variantAndName } from "./GjennomforingStatusTag";

interface Props {
  status: string;
  avbrutt?: {
      tidspunkt: string;
      aarsak: string;
      beskrivelse: string;
  };
}

export function GjennomforingStatusMedAarsakTag({ status, avbrutt }: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const { variant, label } = variantAndName(status);
  const labelWithBeskrivelse = avbrutt?.beskrivelse
    ? `${label} - ${avbrutt.beskrivelse}`
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
