import styles from "./ToolbarButtonRow.module.scss";
import { PropsWithChildren } from "react";

export function ToolbarButtonRow(props: PropsWithChildren) {
  return <div className={styles.button_row}>{props.children}</div>;
}
