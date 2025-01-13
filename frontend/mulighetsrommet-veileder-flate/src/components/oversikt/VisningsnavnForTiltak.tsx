import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import classNames from "classnames";

interface Props {
  tiltakstypeNavn: string;
  navn: string;
  noLink?: boolean;
}

export function VisningsnavnForTiltak({ navn, tiltakstypeNavn, noLink = false }: Props): ReactNode {
  return (
    <div
      className={classNames(styles.container, {
        [styles.no_link]: noLink,
      })}
    >
      <BodyShort textColor="default" weight="semibold" size="small">
        {tiltakstypeNavn}
      </BodyShort>
      <BodyShort size="small">{navn}</BodyShort>
    </div>
  );
}
