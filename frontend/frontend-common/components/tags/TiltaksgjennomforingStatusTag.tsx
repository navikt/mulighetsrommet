import { Tag } from "@navikt/ds-react";
import { AvbrytGjennomforingAarsak, TiltaksgjennomforingStatusResponse } from "mulighetsrommet-api-client";
import { useState } from "react";

interface Props {
  status: TiltaksgjennomforingStatusResponse;
  showAvbruttAarsak?: boolean;
}

export function TiltaksgjennomforingStatusTag({
  status,
  showAvbruttAarsak = false,
}: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  function variantAndName(): { variant: "alt1" | "success" | "neutral" | "error"; name: string } {
    switch (status.name) {
      case "GJENNOMFORES":
        return { variant: "success", name: "Gjennomføres" };
      case "AVSLUTTET":
        return { variant: "neutral", name: "Avsluttet" };
      case "AVBRUTT":
        return { variant: "error", name: "Avbrutt" };
      case "AVLYST":
        return { variant: "error", name: "Avlyst" };
      case "PLANLAGT":
        return { variant: "alt1", name: "Planlagt" };
    }
  }
  const { variant, name } = variantAndName();

  function labelText(): string {
    if ((status.name === "AVBRUTT" || status.name === "AVLYST") && showAvbruttAarsak) {
      return `${name} - ${avbrytGjennomforingAarsakToString(status.aarsak)}`;
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

function avbrytGjennomforingAarsakToString(
  aarsak: AvbrytGjennomforingAarsak | string,
): string {
  switch (aarsak) {
    case AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA:
      return "Avbrutt i Arena";
    case AvbrytGjennomforingAarsak.BUDSJETT_HENSYN:
      return "Budsjetthensyn";
    case AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR:
      return "Endring hos arrangør";
    case AvbrytGjennomforingAarsak.FEILREGISTRERING:
      return "Feilregistrering";
    case AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE:
      return "For få deltakere";
    default:
      return aarsak;
  }
}

