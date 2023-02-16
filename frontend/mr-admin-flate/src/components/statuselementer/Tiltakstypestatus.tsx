import { Tag } from "@navikt/ds-react";
import { Tiltakstype } from "mulighetsrommet-api-client";
import { kalkulerStatusBasertPaaFraOgTilDato } from "../../utils/Utils";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstypestatus({ tiltakstype }: Props) {
  const status = kalkulerStatusBasertPaaFraOgTilDato({
    fraDato: tiltakstype.fraDato,
    tilDato: tiltakstype.tilDato,
  });

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
