import { Tag } from "@navikt/ds-react";
import { Tiltakstype, Tiltakstypestatus } from "mulighetsrommet-api-client";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypestatusTag({ tiltakstype }: Props) {
  const status = tiltakstype.status;

  const variant = (status: Tiltakstypestatus) => {
    switch (status) {
      case Tiltakstypestatus.AKTIV: return "success"
      case Tiltakstypestatus.PLANLAGT: return "alt1"
      case Tiltakstypestatus.AVSLUTTET: return "neutral"
    }
  }

  return (
    <Tag
      size="small"
      aria-label={`Status for tiltakstype: ${status}`}
      variant={variant(status)}
    >
      {status}
    </Tag>
  );
}
