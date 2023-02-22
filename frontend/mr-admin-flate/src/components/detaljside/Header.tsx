import { Heading } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode } from "react";
import { Tilbakelenke } from "../navigering/Tilbakelenke";
import styles from "./Header.module.scss";

interface Props {
  children: ReactNode;
}

export function Header({ children }: Props) {
  return (
    <header className={classNames(styles.header, styles.padding_detaljer)}>
      <Tilbakelenke>Tilbake</Tilbakelenke>
      <Heading size="large" level="2">
        {children}
      </Heading>
    </header>
  );
}
