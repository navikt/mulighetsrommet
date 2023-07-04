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

export const Separator = () =>
  <hr className={styles.separator} />;

export const VerticalSeparator = () =>
  <div className={styles.vertical_separator} />;

interface ListeProps {
  elementer: { key: string; value: string }[];
  tekstHvisTom: string;
}

export function Liste({ elementer, tekstHvisTom }: ListeProps) {
  if (elementer.length === 0) return tekstHvisTom;
  return (
    <ul>
      {elementer.map(({ key, value }) => (
        <li key={key}>{value}</li>
      ))}
    </ul>
  );
}
