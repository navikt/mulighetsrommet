import { Tag } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { useState } from "react";
import { avbrytAvtaleAarsakToString } from "@/utils/Utils";

interface Props {
  avtale: Avtale;
  showAvbruttAarsak?: boolean;
}

export function AvtalestatusTag({ avtale, showAvbruttAarsak = false }: Props) {
  const { status } = avtale;
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  function variant() {
    switch (status.name) {
      case "AKTIV":
        return "success";
      case "AVSLUTTET":
        return "neutral";
      case "AVBRUTT":
        return "error";
    }
  }

  function labelText(): string {
    if (status.name === "AVBRUTT" && showAvbruttAarsak) {
      return `${status.name} - ${avbrytAvtaleAarsakToString(status.aarsak)}`;
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
      aria-label={`Avtalestatus: ${status.name}`}
      variant={variant()}
    >
      {expandLabel ? label : slicedLabel}
    </Tag>
  );
}
