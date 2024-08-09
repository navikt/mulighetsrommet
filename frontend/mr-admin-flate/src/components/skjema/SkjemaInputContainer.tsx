import { PropsWithChildren } from "react";
import styles from "./SkjemaInputContainer.module.scss";

export function SkjemaInputContainer(props: PropsWithChildren) {
  return <div className={styles.input_container}>{props.children}</div>;
}
