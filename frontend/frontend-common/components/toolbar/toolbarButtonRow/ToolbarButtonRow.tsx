import styles from "./ToolbarButtonRow.module.scss";
import { PropsWithChildren } from "react";

export const ToolbarButtonRow = (props: PropsWithChildren) => {
  return <div className={styles.button_row}>{props.children}</div>;
};
