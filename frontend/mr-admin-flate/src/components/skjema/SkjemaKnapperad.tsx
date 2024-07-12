import { PropsWithChildren } from "react";
import styles from "./SkjemaKnapperad.module.scss";

export function SkjemaKnapperad(props: PropsWithChildren) {
  return <div className={styles.skjema_knapperad}>{props.children}</div>;
}
