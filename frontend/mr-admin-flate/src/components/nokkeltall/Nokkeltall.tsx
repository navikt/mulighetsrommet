import { BodyShort, Heading } from "@navikt/ds-react";
import classNames from "classnames";
import styles from "./Nokkeltall.module.scss";

interface Props {
  title: string;
  subtitle: string;
  value: string;
}

export function Nokkeltall({ title, subtitle, value }: Props) {
  return (
    <BodyShort
      className={classNames(styles.nokkeltall, styles.nokkeltall_container)}
    >
      <Heading level="3" size="xsmall" className={styles.heading}>
        {title}
      </Heading>
      <span className={styles.value}>{value}</span>
      <span className={styles.muted}>{subtitle}</span>
    </BodyShort>
  );
}
