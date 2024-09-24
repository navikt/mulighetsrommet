import { Tag } from "@navikt/ds-react";
import { AvtaleDto } from "@mr/api-client";
import { useState } from "react";
import { avbrytAvtaleAarsakToString } from "@/utils/Utils";

interface Props {
  avtale: AvtaleDto;
  showAvbruttAarsak?: boolean;
}

export function AvtalestatusTag({ avtale, showAvbruttAarsak = false }: Props) {
  const { status } = avtale;
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  function variantAndName(): { variant: "success" | "neutral" | "error"; name: string } {
    switch (status.name) {
      case "AKTIV":
        return { variant: "success", name: "Aktiv" };
      case "AVSLUTTET":
        return { variant: "neutral", name: "Avsluttet" };
      case "AVBRUTT":
        return { variant: "error", name: "Avbrutt" };
    }
  }
  const { variant, name } = variantAndName();

  function labelText(): string {
    if (status.name === "AVBRUTT" && showAvbruttAarsak) {
      return `${name} - ${avbrytAvtaleAarsakToString(status.aarsak)}`;
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
      aria-label={`Avtalestatus: ${name}`}
      variant={variant}
    >
      {expandLabel ? label : slicedLabel}
    </Tag>
  );
}
