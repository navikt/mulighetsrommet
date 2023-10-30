import { Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./Header.module.scss";

interface Props {
  children: ReactNode;
}

export function Header({ children }: Props) {
  return (
    <div className={styles.header_container}>
      <div className={styles.header}>
        <Heading size="large" level="2">
          {children}
        </Heading>
      </div>
    </div>
  );
}
