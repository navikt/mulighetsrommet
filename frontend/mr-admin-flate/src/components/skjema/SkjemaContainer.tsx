import { PropsWithChildren } from "react";
import styles from "./SkjemaContainer.module.scss";

export function SkjemaContainer(props: PropsWithChildren) {
  return <div className={styles.skjemacontainer}>{props.children}</div>;
}
