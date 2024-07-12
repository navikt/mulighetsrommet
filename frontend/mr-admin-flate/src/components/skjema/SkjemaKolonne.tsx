import { PropsWithChildren } from "react";
import styles from "./SkjemaKolonne.module.scss";

export function SkjemaKolonne(props: PropsWithChildren) {
  return <div className={styles.skjemakolonne}>{props.children}</div>;
}
