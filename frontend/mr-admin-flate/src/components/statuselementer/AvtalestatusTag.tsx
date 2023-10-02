import { Tag } from "@navikt/ds-react";
import { Avtale, Avtalestatus } from "mulighetsrommet-api-client";

interface Props {
  avtale: Avtale;
}

export function AvtalestatusTag({ avtale }: Props) {
  const { avtalestatus } = avtale;

  const variant = (status: Avtalestatus) => {
    switch (status) {
      case Avtalestatus.AKTIV:
        return "success";
      case Avtalestatus.PLANLAGT:
        return "alt1";
      case Avtalestatus.AVSLUTTET:
        return "neutral";
      case Avtalestatus.AVBRUTT:
        return "error";
    }
  };

  return (
    <>
      <Tag
        size="small"
        aria-label={`Avtalestatus: ${avtalestatus}`}
        variant={variant(avtalestatus)}
      >
        {avtalestatus}
      </Tag>
    </>
  );
}
