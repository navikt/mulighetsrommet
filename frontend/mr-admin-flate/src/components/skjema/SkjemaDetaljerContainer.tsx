import { PropsWithChildren } from "react";
import styles from "./DetaljerContainer.module.scss";

export function SkjemaDetaljerContainer(props: PropsWithChildren) {
  return <div className={styles.skjema_detaljer_container}>{props.children}</div>;
}
