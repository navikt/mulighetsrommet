import { PropsWithChildren } from "react";
import styles from "./RedaksjoneltInnholdContainer.module.scss";

export function RedaksjoneltInnholdContainer(props: PropsWithChildren) {
  return (
    <div className={styles.container}>
      {props.children}
      <hr className={styles.separator} />
    </div>
  );
}
