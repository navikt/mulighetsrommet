import { PropsWithChildren } from "react";
import styles from "../skjema/Skjema.module.scss";
import classNames from "classnames";

export function FormGroup(props: PropsWithChildren) {
  return (
    <div className={classNames(styles.form_group, styles.form_group_grey)}>{props.children}</div>
  );
}
