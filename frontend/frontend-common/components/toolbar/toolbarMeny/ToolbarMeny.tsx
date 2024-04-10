import styles from "./ToolbarMeny.module.scss";
import { PropsWithChildren } from "react";

export function ToolbarMeny(props: PropsWithChildren) {
  return <div className={styles.toolbar_meny}>{props.children}</div>;
}
