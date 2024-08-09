import { PropsWithChildren } from "react";
import styles from "./RedaksjoneltInnholdContainer.module.scss";

interface Props {
  separator?: boolean;
}
export function RedaksjoneltInnholdContainer({
  separator = false,
  children,
}: PropsWithChildren<Props>) {
  return (
    <div className={styles.redaksjonelt_innhold_container}>
      {children}
      {separator ? <hr className={styles.separator} /> : null}
    </div>
  );
}
