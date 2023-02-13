import { Tag } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { kalkulerStatusBasertPaaFraOgTilDato } from "../../utils/Utils";

interface Props {
  avtale: Avtale;
}

export function Avtalestatus({ avtale }: Props) {
  const status = kalkulerStatusBasertPaaFraOgTilDato({
    fraDato: avtale.startDato,
    tilDato: avtale.sluttDato,
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
