import { ReactNode } from "react";
import styles from "./ContainerLayout.module.scss";

interface Props {
  children: ReactNode;
}

export function ContainerLayout({ children }: Props) {
  return <div className={styles.container_layout}>{children}</div>;
}
