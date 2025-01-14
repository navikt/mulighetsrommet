import { Tag } from "@navikt/ds-react";
import { GjennomforingStatus } from "@mr/api-client-v2";

interface Props {
  status: GjennomforingStatus;
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

export function variantAndName(status: GjennomforingStatus): {
  variant: "alt1" | "success" | "neutral" | "error";
  label: string;
} {
  switch (status) {
    case GjennomforingStatus.GJENNOMFORES:
      return { variant: "success", label: "Gjennomføres" };
    case GjennomforingStatus.AVSLUTTET:
      return { variant: "neutral", label: "Avsluttet" };
    case GjennomforingStatus.AVBRUTT:
      return { variant: "error", label: "Avbrutt" };
    case GjennomforingStatus.AVLYST:
      return { variant: "error", label: "Avlyst" };
  }
}
