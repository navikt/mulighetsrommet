import { PropsWithChildren } from "react";
import styles from "./LokalInformasjonContainer.module.scss";

export function LokalInformasjonContainer(props: PropsWithChildren) {
  return <div className={styles.lokal_informasjon_container}>{props.children}</div>;
}
