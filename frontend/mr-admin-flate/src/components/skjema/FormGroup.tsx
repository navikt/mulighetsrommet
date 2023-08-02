import { ReactNode } from "react";
import styles from "../skjema/Skjema.module.scss";
import classNames from "classnames";

export const FormGroup = ({
  children,
  cols = 1,
}: {
  children: ReactNode;
  cols?: number;
}) => (
  <div className={styles.form_group}>
    <div
      className={classNames(styles.grid, {
        [styles.grid_1]: cols === 1,
        [styles.grid_2]: cols === 2,
      })}
    >
      {children}
    </div>
  </div>
);
