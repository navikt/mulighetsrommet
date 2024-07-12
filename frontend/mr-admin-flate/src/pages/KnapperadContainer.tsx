import styles from "./KnapperadContainer.module.scss";
import { PropsWithChildren } from "react";

export function KnapperadContainer(props: PropsWithChildren) {
  return <div className={styles.knapperad_container}>{props.children}</div>;
}
