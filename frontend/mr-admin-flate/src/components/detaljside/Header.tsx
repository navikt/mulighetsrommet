import { Heading } from "@navikt/ds-react";
import { ReactNode } from "react";
import { Tilbakelenke } from "../navigering/Tilbakelenke";
import styles from "./Header.module.scss";

interface Props {
  children: ReactNode;
  dataTestId?: string;
  onClickLink?: () => void;
}

export function Header({ children, dataTestId, onClickLink }: Props) {
  return (
    <div className={styles.header_container} data-testid={dataTestId}>
      <div className={styles.header}>
        <Tilbakelenke onClick={onClickLink} />
        <Heading size="large" level="2">
          {children}
        </Heading>
      </div>
    </div>
  );
}
