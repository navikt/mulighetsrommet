import { PropsWithChildren } from "react";
import styles from "./InfoContainer.module.scss";

interface Props {
  dataTestId?: string;
}
export function InfoContainer({ dataTestId, children }: PropsWithChildren<Props>) {
  return (
    <div className={styles.info_container} data-testid={dataTestId}>
      {children}
    </div>
  );
}
