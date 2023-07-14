import { ReactNode } from "react";
import styles from "./ContainerLayout.module.scss";

interface Props {
  children: ReactNode;
}

export function ContainerLayoutOversikt({ children }: Props) {
  return <div className={styles.container_layout_oversikt}>{children}</div>;
}

export function ContainerLayoutDetaljer({ children }: Props) {
  return <div className={styles.container_layout_detaljer}>{children}</div>;
}
