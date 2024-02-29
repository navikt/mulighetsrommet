import { ReactNode } from "react";
import styles from "../skjema/Skjema.module.scss";
import classNames from "classnames";

export const FormGroup = ({ children }: { children: ReactNode }) => (
  <div className={styles.form_group}>
    <div className={classNames(styles.grid)}>{children}</div>
  </div>
);
