import { ReactNode } from "react";
import styles from "./Header.module.scss";
import classNames from "classnames";

interface Props {
  children: ReactNode;
  harForhandsvisningsknapp?: boolean;
}

export function Header({ children, harForhandsvisningsknapp = false }: Props) {
  return (
    <div
      className={classNames(
        styles.header_container,
        harForhandsvisningsknapp ? styles.header_container_forhandsvisningsknapp : null,
      )}
    >
      <div className={styles.header}>{children}</div>
    </div>
  );
}
