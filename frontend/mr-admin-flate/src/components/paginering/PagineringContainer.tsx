import styles from "./Paginering.module.scss";
import { PropsWithChildren } from "react";

export function PagineringContainer(props: PropsWithChildren) {
  return <div className={styles.paginering}>{props.children}</div>;
}
