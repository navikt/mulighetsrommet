import { ReactNode } from "react";
import styles from "./Bolk.module.scss";

export function Bolk({ children, ...rest }: { children: ReactNode }) {
  return (
    <dl className={styles.bolk} {...rest}>
      {children}
    </dl>
  );
}
