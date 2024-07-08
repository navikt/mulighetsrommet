import { PropsWithChildren } from "react";
import styles from "./FaneinnholdContainer.module.scss";

export function FaneinnholdContainer(props: PropsWithChildren) {
  return <div className={styles.faneinnhold_container}>{props.children}</div>;
}
