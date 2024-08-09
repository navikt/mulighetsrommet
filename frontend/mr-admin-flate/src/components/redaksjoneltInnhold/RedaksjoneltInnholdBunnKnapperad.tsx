import { PropsWithChildren } from "react";
import styles from "./RedaksjoneltInnholdBunnKnapperad.module.scss";

export function RedaksjoneltInnholdBunnKnapperad(props: PropsWithChildren) {
  return <div className={styles.redaksjonelt_innhold_bunn_knapperad}>{props.children}</div>;
}
