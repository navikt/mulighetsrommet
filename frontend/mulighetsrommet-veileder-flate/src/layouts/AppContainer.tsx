import styles from "./AppContainer.module.scss";
import React, { ReactNode } from "react";
import { ErrorFallback } from "@/utils/ErrorFallback";
import { ErrorBoundary } from "react-error-boundary";
import { TiltakLoader } from "@/components/TiltakLoader";

interface Props {
  header?: ReactNode;
  children: ReactNode;
}

export const AppContainer = ({ children, header }: Props) => {
  return (
    <div className={styles.app_container}>
      {header}
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <React.Suspense fallback={<TiltakLoader />}>
          <div className={styles.app_content}>{children}</div>
        </React.Suspense>
      </ErrorBoundary>
    </div>
  );
};
