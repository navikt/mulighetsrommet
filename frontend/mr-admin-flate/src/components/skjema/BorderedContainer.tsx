import { PropsWithChildren } from "react";
import styles from "@/components/skjema/BorderedContainer.module.scss";

export function BorderedContainer(props: PropsWithChildren) {
  return <div className={styles.bordered_container}> {props.children}</div>;
}
