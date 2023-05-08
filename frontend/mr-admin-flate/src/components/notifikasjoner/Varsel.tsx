import { BellIcon } from "@navikt/aksel-icons";
import { LinkPanel } from "@navikt/ds-react";
import styles from "./Varsel.module.scss";

interface IVarsel {
  tittel: string;
  melding: string;
  href: string;
}

export function Varsel({ tittel, melding, href }: IVarsel) {
  return (
    <>
      <LinkPanel href={href} className={styles.container} border={false}>
        <div className={styles.flex}>
          <div>
            <span className={styles.ikon_container}>
              <BellIcon className={styles.ikon} />
            </span>
          </div>
          <div>
            <LinkPanel.Title as="h3">{tittel}</LinkPanel.Title>
            <LinkPanel.Description>{melding}</LinkPanel.Description>
          </div>
        </div>
      </LinkPanel>
    </>
  );
}
