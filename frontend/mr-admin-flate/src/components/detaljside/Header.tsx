import { Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./Header.module.scss";

interface Props {
  children: ReactNode;
  dataTestId?: string;
}

export function Header({ children, dataTestId }: Props) {
  return (
    <div className={styles.header_container} data-testid={dataTestId}>
      <div className={styles.header}>
        <Heading size="large" level="2">
          {children}
        </Heading>
      </div>
    </div>
  );
}
