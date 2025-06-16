import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import classNames from "classnames";

interface Props {
  tiltakstypeNavn: string;
  navn: string;
  noLink?: boolean;
}

export function VisningsnavnForTiltak({ navn, tiltakstypeNavn, noLink = false }: Props): ReactNode {
  return (
    <div>
      <BodyShort
        className={classNames(
          `overflow-hidden overflow-ellipsis whitespace-nowrap text-[#0067c5]`,
          {
            "text-[#000000]": noLink,
          },
        )}
        textColor="default"
        weight="semibold"
        size="small"
      >
        {tiltakstypeNavn}
      </BodyShort>
      <BodyShort size="small">{navn}</BodyShort>
    </div>
  );
}
