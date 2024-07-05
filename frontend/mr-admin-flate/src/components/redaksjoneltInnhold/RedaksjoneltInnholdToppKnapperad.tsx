import { PropsWithChildren } from "react";
import styles from "./RedaksjoneltInnholdToppKnapperad.module.scss";

export function RedaksjoneltInnholdToppKnapperad(props: PropsWithChildren) {
  return (
    <div className={styles.redaksjonelt_innhold_topp_knapperad}>
      {props.children}
      <hr className={styles.separator} />
    </div>
  );
}
