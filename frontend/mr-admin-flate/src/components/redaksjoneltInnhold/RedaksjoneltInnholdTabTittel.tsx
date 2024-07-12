import { PropsWithChildren } from "react";
import styles from "./RedaksjoneltInnholdTabTittel.module.scss";

export function RedaksjoneltInnholdTabTittel(props: PropsWithChildren) {
  return <div className={styles.redaksjonelt_innhold_tab_tittel}>{props.children}</div>;
}
