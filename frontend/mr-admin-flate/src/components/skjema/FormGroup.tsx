import { PropsWithChildren } from "react";
import styles from "../skjema/Skjema.module.scss";

export function FormGroup(props: PropsWithChildren) {
  return <div className={styles.form_group}>{props.children}</div>;
}
