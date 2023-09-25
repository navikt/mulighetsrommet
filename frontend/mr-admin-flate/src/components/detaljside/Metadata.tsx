import { ReactNode } from "react";
import styles from "./Metadata.module.scss";

export function Metadata({
  header,
  verdi = "",
}: {
  header: string | ReactNode;
  verdi: string | number | undefined | null | ReactNode;
}) {
  return (
    <div className={styles.header_og_verdi}>
      <dt className={styles.bold}>{header}</dt>
      <dd className={styles.definition}>{verdi ?? "N/A"}</dd>
    </div>
  );
}

export function Separator() {
  return <hr className={styles.separator} />;
}
