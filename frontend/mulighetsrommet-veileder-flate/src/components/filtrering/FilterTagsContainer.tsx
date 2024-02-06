import styles from "@/components/filtrering/FilterTagsContainer.module.scss";
import { PropsWithChildren } from "react";

export function FilterTagsContainer(props: PropsWithChildren) {
  return (
    <div className={styles.filtertags} data-testid="filtertags">
      {props.children}
    </div>
  );
}
