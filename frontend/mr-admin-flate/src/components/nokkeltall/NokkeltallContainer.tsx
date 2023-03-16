import { ReactNode } from "react";
import styles from "./NokkeltallContainer.module.scss";

export function NokkeltallContainer({ children }: { children: ReactNode }) {
  return (
    <div>
      <section className={styles.summary_container}>{children}</section>
    </div>
  );
}
