import { Tag } from "@navikt/ds-react";
import { TiltaksgjennomforingStatus } from "@mr/api-client";

interface Props {
  status: TiltaksgjennomforingStatus;
}

export function GjennomforingStatusTag({ status }: Props) {
  const { variant, label } = variantAndName(status);

  return (
    <Tag size="small" aria-label={`Gjennomføringstatus: ${label}`} variant={variant}>
      {label}
    </Tag>
  );
}

export function variantAndName(status: TiltaksgjennomforingStatus): {
  variant: "alt1" | "success" | "neutral" | "error";
  label: string;
} {
  switch (status) {
    case TiltaksgjennomforingStatus.GJENNOMFORES:
      return { variant: "success", label: "Gjennomføres" };
    case TiltaksgjennomforingStatus.AVSLUTTET:
      return { variant: "neutral", label: "Avsluttet" };
    case TiltaksgjennomforingStatus.AVBRUTT:
      return { variant: "error", label: "Avbrutt" };
    case TiltaksgjennomforingStatus.AVLYST:
      return { variant: "error", label: "Avlyst" };
  }
}
