import { Tag } from "@navikt/ds-react";
import { Tiltakstype, TiltakstypeStatus } from "mulighetsrommet-api-client";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypestatusTag({ tiltakstype }: Props) {
  const status = tiltakstype.status;

  function variantAndName(): { variant: "success" | "neutral"; name: string } {
    switch (status) {
      case TiltakstypeStatus.AKTIV:
        return { variant: "success", name: "Aktiv" };
      case TiltakstypeStatus.AVSLUTTET:
        return { variant: "neutral", name: "Avsluttet" };
    }
  }

  const { variant, name } = variantAndName();

  return (
    <Tag size="small" aria-label={`Status for tiltakstype: ${name}`} variant={variant}>
      {name}
    </Tag>
  );
}
