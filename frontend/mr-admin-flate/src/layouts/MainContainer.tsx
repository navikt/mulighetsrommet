import { ReactNode } from "react";
import styles from "./MainContainer.module.scss";

export function MainContainer({ children }: { children: ReactNode }) {
  return <main className={styles.container}>{children}</main>;
}
