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
      className={classNames(styles.header_container, {
        [styles.header_container_forhandsvisningsknapp]: harForhandsvisningsknapp,
      })}
    >
      <div className={styles.header}>{children}</div>
    </div>
  );
}
