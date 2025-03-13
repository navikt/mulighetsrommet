import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  tiltakstypeNavn: string;
  navn: string;
  noLink?: boolean;
}

export function VisningsnavnForTiltak({ navn, tiltakstypeNavn, noLink = false }: Props): ReactNode {
  return (
    <div>
      <BodyShort
        textColor="default"
        weight="semibold"
        size="small"
        className={`truncate ${noLink ? "text-black" : "text-[#0067c5]"}`}
      >
        {tiltakstypeNavn}
      </BodyShort>
      <BodyShort size="small">{navn}</BodyShort>
    </div>
  );
}
