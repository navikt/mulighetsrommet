import { PropsWithChildren } from "react";
import styles from "./DescriptionRichtextContainer.module.scss";

export function DescriptionRichtextContainer(props: PropsWithChildren) {
  return <div className={styles.description_richtext_container}>{props.children}</div>;
}
