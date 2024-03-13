import { Tag } from "@navikt/ds-react";
import { Tiltaksgjennomforing, TiltaksgjennomforingStatus } from "mulighetsrommet-api-client";
import { oversettStatusForTiltaksgjennomforing } from "../../utils/Utils";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingstatusTag({ tiltaksgjennomforing }: Props) {
  const status = tiltaksgjennomforing.status;

  const variant = (status: TiltaksgjennomforingStatus | undefined) => {
    switch (status) {
      case TiltaksgjennomforingStatus.GJENNOMFORES:
        return "success";
      case TiltaksgjennomforingStatus.AVBRUTT:
        return "error";
      case TiltaksgjennomforingStatus.AVLYST:
        return "neutral";
      case TiltaksgjennomforingStatus.AVSLUTTET:
        return "neutral";
      case TiltaksgjennomforingStatus.PLANLAGT:
        return "alt1";
      case undefined:
        return "neutral";
    }
  };

  return (
    <Tag
      size="small"
      aria-label={`Status for tiltaksgjennomføring: ${oversettStatusForTiltaksgjennomforing(status)}`}
      variant={variant(status)}
    >
      {oversettStatusForTiltaksgjennomforing(status)}
    </Tag>
  );
}
