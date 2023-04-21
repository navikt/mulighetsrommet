import { ReactNode } from "react";
import styles from "./Metadata.module.scss";

type Verdi = string | number | undefined | null | ReactNode | string[];

function renderVerdi(verdi: Verdi) {
  if (Array.isArray(verdi)) {
    return (
      <ul className={styles.unstyled_list}>
        {verdi.map((v, index) => (
          <li key={index}>{v}</li>
        ))}
      </ul>
    );
  }
  return verdi;
}

export function Metadata({
  header,
  verdi = "",
}: {
  header: string;
  verdi: Verdi;
}) {
  return (
    <div className={styles.header_og_verdi}>
      <dt className={styles.bold}>{header}</dt>
      <dd className={styles.definition}>{renderVerdi(verdi)}</dd>
    </div>
  );
}

export function Separator() {
  return <hr className={styles.separator} />;
}
