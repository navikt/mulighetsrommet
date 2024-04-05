import { ReactNode } from "react";
import styles from "../skjema/Skjema.module.scss";
import classNames from "classnames";

export function FormGroup({ children }: { children: ReactNode }) {
  return (
    <div className={styles.form_group}>
      <div className={classNames(styles.grid)}>{children}</div>
    </div>
  );
}
