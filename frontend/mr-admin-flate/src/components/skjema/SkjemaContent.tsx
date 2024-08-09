import { PropsWithChildren } from "react";
import styles from "./SkjemaContent.module.scss";

export function SkjemaContent(props: PropsWithChildren) {
  return <div className={styles.skjema_content}>{props.children}</div>;
}
