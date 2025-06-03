import { Tag } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  status: "UTKAST" | "AKTIV" | "AVSLUTTET" | "AVBRUTT";
  beskrivelse?: string;
}

export function AvtaleStatusTag({ status, beskrivelse }: Props) {
  const [expandLabel, setExpandLabel] = useState<boolean>(false);

  const { variant, name } = getVariantAndName(status);
  const label = beskrivelse ? `${name} - ${beskrivelse}` : name;

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

function getVariantAndName(status: Props["status"]): {
  variant: "success" | "neutral" | "error";
  name: string;
} {
  switch (status) {
    case "AKTIV":
      return { variant: "success", name: "Aktiv" };
    case "AVSLUTTET":
      return { variant: "neutral", name: "Avsluttet" };
    case "AVBRUTT":
      return { variant: "error", name: "Avbrutt" };
    case "UTKAST":
      return { variant: "neutral", name: "Utkast" };
  }
}

function truncate(text: string, maxLength: number): string {
  return text.length > maxLength ? `${text.substring(0, maxLength - 3)}...` : text;
}
