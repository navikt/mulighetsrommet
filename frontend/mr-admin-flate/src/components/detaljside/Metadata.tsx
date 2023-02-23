import { ReactNode } from "react";
import styles from "./Metadata.module.scss";

export function Grid({
  children,
  as,
}: {
  children: ReactNode;
  as: React.ElementType;
}) {
  const As = as;
  return <As className={styles.grid}>{children}</As>;
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
