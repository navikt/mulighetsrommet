import { Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import { Tilbakelenke } from "../navigering/Tilbakelenke";
import styles from "./Header.module.scss";

interface Props {
  children: ReactNode;
}

export function Header({ children }: Props) {
  return (
    <header className={styles.header_container}>
      <div className={styles.header}>
        <Tilbakelenke>Tilbake</Tilbakelenke>
        <Heading size="large" level="2">
          {children}
        </Heading>
      </div>
    </header>
  );
}
