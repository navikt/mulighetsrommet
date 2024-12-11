import { BodyShort } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./VisningsnavnForTiltak.module.scss";
import classNames from "classnames";

interface Props {
  tittel: string;
  underTittel: string;
  noLink?: boolean;
}

export function VisningsnavnForTiltak({ tittel, underTittel, noLink = false }: Props): ReactNode {
  return (
    <div
      className={classNames(styles.container, {
        [styles.no_link]: noLink,
      })}
    >
      <BodyShort textColor="default" weight="semibold" size="small">
        {underTittel}
      </BodyShort>
      <BodyShort size="small">{tittel}</BodyShort>
    </div>
  );
}
