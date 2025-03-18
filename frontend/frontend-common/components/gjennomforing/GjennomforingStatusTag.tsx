import { Tag } from "@navikt/ds-react";

interface Props {
  status: string;
}

export function GjennomforingStatusTag({ status }: Props) {
  const { variant, label } = variantAndName(status);

  return (
    <Tag
      size="small"
      className="w-[140px] text-center whitespace-nowrap"
      aria-label={`Gjennomføringstatus: ${label}`}
      variant={variant}
    >
      {label}
    </Tag>
  );
}

export function variantAndName(status: string): {
  variant: "alt1" | "success" | "neutral" | "error";
  label: string;
} {
  const statusMap: Record<string, { variant: "alt1" | "success" | "neutral" | "error"; label: string }> = {
    GJENNOMFORES: { variant: "success", label: "Gjennomføres" },
    AVSLUTTET: { variant: "neutral", label: "Avsluttet" },
    AVBRUTT: { variant: "error", label: "Avbrutt" },
    AVLYST: { variant: "error", label: "Avlyst" },
  };

  return statusMap[status] || { variant: "alt1", label: "Ukjent status" };
}
