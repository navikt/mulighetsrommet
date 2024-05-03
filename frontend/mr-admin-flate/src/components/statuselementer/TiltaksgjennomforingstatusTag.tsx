import { avbrytGjennomforingAarsakToString } from "@/utils/Utils";
import { Tag } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useState } from "react";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  showAvbruttAarsak?: boolean;
}

export function TiltaksgjennomforingstatusTag({
  tiltaksgjennomforing,
  showAvbruttAarsak = false,
}: Props) {
  const { status } = tiltaksgjennomforing;
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  function variant() {
    switch (status.name) {
      case "GJENNOMFORES":
        return "success";
      case "AVBRUTT":
      case "AVLYST":
        return "error";
      case "AVSLUTTET":
        return "neutral";
      case "PLANLAGT":
        return "alt1";
    }
  }

  function labelText(): string {
    if ((status.name === "AVBRUTT" || status.name === "AVLYST") && showAvbruttAarsak) {
      return `${status.name} - ${avbrytGjennomforingAarsakToString(status.aarsak)}`;
    }

    return status.name;
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
      aria-label={`Gjennomføringstatus: ${status.name}`}
      variant={variant()}
    >
      {expandLabel ? label : slicedLabel}
    </Tag>
  );
}
