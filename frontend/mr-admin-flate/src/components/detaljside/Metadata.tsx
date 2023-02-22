import { ReactNode } from "react";
import styles from "./Metadata.module.scss";

export function Grid({ children }: { children: ReactNode }) {
  return <div className={styles.grid}>{children}</div>;
}

export function Metadata({
  header,
  verdi = "",
}: {
  header: string;
  verdi: string | number | undefined | null | ReactNode;
}) {
  return (
    <div className={styles.header_og_verdi}>
      <dt className={styles.bold}>{header}</dt>
      <dd className={styles.definition}>{verdi}</dd>
    </div>
  );
}

export function Separator() {
  return <hr className={styles.separator} />;
}
