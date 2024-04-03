import { ReactNode } from "react";
import styles from "./Paginering.module.scss";

interface Props {
  children: ReactNode;
}

export function PagineringContainer({ children }: Props) {
  return <div className={styles.paginering}>{children}</div>;
}
