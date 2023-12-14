import { ReactNode } from "react";
import styles from "./Metadata.module.scss";
import classNames from "classnames";

export interface MetadataProps {
  header: string | ReactNode;
  verdi: string | number | undefined | null | ReactNode;
}

export function Metadata({ header, verdi }: MetadataProps) {
  return (
    <div className={styles.header_og_verdi}>
      <dt className={styles.bold}>{header}</dt>
      <dd className={styles.definition}>{verdi ?? "N/A"}</dd>
    </div>
  );
}

export function Separator({ classname }: { classname?: string }) {
  return <hr className={classNames(styles.separator, classname)} />;
}
