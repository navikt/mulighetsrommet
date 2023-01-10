import { ReactNode } from "react";
import styles from "./RootLayout.module.scss";

export function RootLayout({ children }: { children: ReactNode }) {
  return <main className={styles.container}>{children}</main>;
}
