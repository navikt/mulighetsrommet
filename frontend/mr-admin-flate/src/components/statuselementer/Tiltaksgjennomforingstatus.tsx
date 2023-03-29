import { Tag } from "@navikt/ds-react";
import {
  Tiltaksgjennomforing,
  TiltaksgjennomforingStatus as TiltaksgjennomforingType,
} from "mulighetsrommet-api-client";
import { oversettStatusForTiltaksgjennomforing } from "../../utils/Utils";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function Tiltaksgjennomforingstatus({ tiltaksgjennomforing }: Props) {
  const status = tiltaksgjennomforing.status;

  return (
    <Tag
      aria-label={`Status for tiltaksgjennomføring: ${status}`}
      variant={
        status === TiltaksgjennomforingType.GJENNOMFORES ? "success" : "neutral"
      }
    >
      {oversettStatusForTiltaksgjennomforing(status)}
    </Tag>
  );
}
