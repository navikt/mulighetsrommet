import { Tag } from "@navikt/ds-react";
import { TiltaksgjennomforingStatus, TiltaksgjennomforingStatusDto } from "@mr/api-client";
import { useState } from "react";

interface Props {
  status: TiltaksgjennomforingStatusDto;
  showAvbruttAarsak?: boolean;
}

export function TiltaksgjennomforingStatusTag({
  status,
  showAvbruttAarsak = false,
}: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  function variantAndName(): { variant: "alt1" | "success" | "neutral" | "error"; name: string } {
    switch (status.status) {
      case TiltaksgjennomforingStatus.GJENNOMFORES:
        return { variant: "success", name: "Gjennomføres" };
      case TiltaksgjennomforingStatus.AVSLUTTET:
        return { variant: "neutral", name: "Avsluttet" };
      case TiltaksgjennomforingStatus.AVBRUTT:
        return { variant: "error", name: "Avbrutt" };
      case TiltaksgjennomforingStatus.AVLYST:
        return { variant: "error", name: "Avlyst" };
      case TiltaksgjennomforingStatus.PLANLAGT:
        return { variant: "alt1", name: "Planlagt" };
    }
  }
  const { variant, name } = variantAndName();

  function labelText(): string {
    if ((status.status === TiltaksgjennomforingStatus.AVBRUTT || status.status === TiltaksgjennomforingStatus.AVLYST) && showAvbruttAarsak) {
      return `${name} - ${status.avbrutt?.beskrivelse}`;
    }

    return name;
  }

  const label = labelText();
  const slicedLabel = label.length > 30 ? label.slice(0, 27) + "..." : label;

  return (
    <Tag
      style={{
        maxWidth: "400px",
      }}
      size="small"
      onMouseEnter={() => setExpandLabel(true)}
      onMouseLeave={() => setExpandLabel(false)}
      aria-label={`Gjennomføringstatus: ${name}`}
      variant={variant}
    >
      {expandLabel ? label : slicedLabel}
    </Tag>
  );
}
