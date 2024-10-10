import { PropsWithChildren } from "react";
import styles from "./InfoContainer.module.scss";
import React from "react";
import { Laster } from "../laster/Laster";

interface Props {
  dataTestId?: string;
}
export function InfoContainer({ dataTestId, children }: PropsWithChildren<Props>) {
  return (
    <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
      <div className={styles.info_container} data-testid={dataTestId}>
        {children}
      </div>
    </React.Suspense>
  );
}
