import { PropsWithChildren } from "react";
import styles from "./ArrangorKontaktinfoContainer.module.scss";

export function ArrangorKontaktinfoContainer(props: PropsWithChildren) {
  return <div className={styles.arrangor_kontaktinfo_container}>{props.children}</div>;
}
