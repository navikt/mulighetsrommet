import styles from "./AppContainer.module.scss";
import { ReactNode } from "react";

interface Props {
  header?: ReactNode;
  children: ReactNode;
}

export const AppContainer = ({ children, header }: Props) => {
  return (
    <div className={styles.app_container}>
      {header}
      <div className={styles.app_content}>{children}</div>
    </div>
  );
};
