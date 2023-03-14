import { BodyShort, Heading, HelpText } from "@navikt/ds-react";
import classNames from "classnames";
import styles from "./Nokkeltall.module.scss";

interface Props {
  title: string;
  subtitle: string;
  value: number | string;
  helptext?: string;
  helptextTitle?: string;
}

export function Nokkeltall({
  title,
  subtitle,
  value,
  helptext,
  helptextTitle,
}: Props) {
  return (
    <div className={classNames(styles.nokkeltall_container)}>
      <div className={styles.nokkeltall}>
        <Heading level="3" size="xsmall" className={styles.heading}>
          {title}
        </Heading>
        <BodyShort className={styles.nokkeltall}>
          <span className={styles.value}>{value}</span>
          <span className={styles.muted}>{subtitle}</span>
        </BodyShort>
      </div>
      {helptext ? <HelpText title={helptextTitle}>{helptext}</HelpText> : null}
    </div>
  );
}
