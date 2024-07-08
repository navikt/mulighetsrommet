import { PropsWithChildren } from "react";
import styles from "./DetaljerContainer.module.scss";

export function DetaljerContainer(props: PropsWithChildren) {
  return <div className={styles.detaljer_container}>{props.children}</div>;
}
