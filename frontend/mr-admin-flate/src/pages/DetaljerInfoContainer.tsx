import { PropsWithChildren } from "react";
import styles from "./DetaljerInfoContainer.module.scss";

export function DetaljerInfoContainer(props: PropsWithChildren) {
  return <div className={styles.detaljer_info_container}>{props.children}</div>;
}
