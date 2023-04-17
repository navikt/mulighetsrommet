import { Tag } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";

interface Props {
  avtale: Avtale;
}

export function Avtalestatus({ avtale }: Props) {
  const { avtalestatus } = avtale;
  return (
    <Tag
      size="small"
      aria-label={`Avtalestatus: ${avtalestatus}`}
      variant={
        avtalestatus === "Aktiv"
          ? "success"
          : avtalestatus === "Planlagt"
          ? "info"
          : "neutral"
      }
    >
      {avtalestatus}
    </Tag>
  );
}
