import { Tag } from "@navikt/ds-react";
import { Tiltakstype } from "mulighetsrommet-api-client";
import { kalkulerStatusForTiltakstype } from "../../utils/Utils";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstypestatus({ tiltakstype }: Props) {
  const status = kalkulerStatusForTiltakstype(tiltakstype);

  return (
    <Tag
      variant={
        status === "Aktiv"
          ? "success"
          : status === "Planlagt"
          ? "info"
          : "neutral"
      }
    >
      {status}
    </Tag>
  );
}
