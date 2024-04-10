import { Tag } from "@navikt/ds-react";
import { Tiltakstype, TiltakstypeStatus } from "mulighetsrommet-api-client";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypestatusTag({ tiltakstype }: Props) {
  const status = tiltakstype.status;

  const variant = (status: TiltakstypeStatus) => {
    switch (status) {
      case TiltakstypeStatus.AKTIV:
        return "success";
      case TiltakstypeStatus.AVSLUTTET:
        return "neutral";
    }
  };

  return (
    <Tag size="small" aria-label={`Status for tiltakstype: ${status}`} variant={variant(status)}>
      {status}
    </Tag>
  );
}
