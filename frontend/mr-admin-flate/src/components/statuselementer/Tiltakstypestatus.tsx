import {Tag} from "@navikt/ds-react";
import {Tiltakstype} from "mulighetsrommet-api-client";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstypestatus({tiltakstype}: Props) {
  const status = tiltakstype.status;

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
