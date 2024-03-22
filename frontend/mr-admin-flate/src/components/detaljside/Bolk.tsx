import { ReactNode } from "react";
import styles from "./Bolk.module.scss";
import classNames from "classnames";

export function Bolk({ children, classez, ...rest }: { children: ReactNode; classez?: string }) {
  return (
    <dl className={classNames(styles.bolk, classez)} {...rest}>
      {children}
    </dl>
  );
}
