import styles from "./AppContainer.module.scss";
import { ReactNode } from "react";
import { ErrorFallback } from "@/utils/ErrorFallback";
import { ErrorBoundary } from "react-error-boundary";

interface Props {
  header?: ReactNode;
  children: ReactNode;
}

export const AppContainer = ({ children, header }: Props) => {
  return (
    <div className={styles.app_container}>
      {header}
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <div className={styles.app_content}>{children}</div>
      </ErrorBoundary>
    </div>
  );
};
