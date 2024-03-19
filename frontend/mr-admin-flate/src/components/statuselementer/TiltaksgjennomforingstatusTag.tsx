import { Tag } from "@navikt/ds-react";
import { Tiltaksgjennomforing, TiltaksgjennomforingStatus } from "mulighetsrommet-api-client";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

type StatusAndVariant = {
  status: "Gjennomføres" | "Avbrutt" | "Avlyst" | "Avsluttet" | "Planlagt";
  variant: "success" | "error" | "neutral" | "alt1";
};

function statusToTag(status: TiltaksgjennomforingStatus): StatusAndVariant {
  switch (status) {
    case TiltaksgjennomforingStatus.GJENNOMFORES:
      return { status: "Gjennomføres", variant: "success" };
    case TiltaksgjennomforingStatus.AVBRUTT:
      return { status: "Avbrutt", variant: "error" };
    case TiltaksgjennomforingStatus.AVLYST:
      return { status: "Avlyst", variant: "neutral" };
    case TiltaksgjennomforingStatus.AVSLUTTET:
      return { status: "Avsluttet", variant: "neutral" };
    case TiltaksgjennomforingStatus.PLANLAGT:
      return { status: "Planlagt", variant: "alt1" };
  }
}

export function TiltaksgjennomforingstatusTag({ tiltaksgjennomforing }: Props) {
  const { status, variant } = statusToTag(tiltaksgjennomforing.status);

  return (
    <Tag size="small" aria-label={`Status for tiltaksgjennomføring: ${status}`} variant={variant}>
      {status}
    </Tag>
  );
}
