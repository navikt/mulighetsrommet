import styles from "./Filtertag.module.scss";
import classNames from "classnames";
import { ReactNode } from "react";

interface Props {
  children: ReactNode;
  filterOpen?: boolean;
}

export function FiltertagsContainer({ children, filterOpen }: Props) {
  return (
    <div
      className={classNames(styles.filtertags, filterOpen ? styles.filtertags_filter_open : "")}
      data-testid="filtertags"
    >
      {children}
    </div>
  );
}
