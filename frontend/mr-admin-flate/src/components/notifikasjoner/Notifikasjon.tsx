import { BellIcon } from "@navikt/aksel-icons";
import { LinkPanel } from "@navikt/ds-react";
import styles from "./Notifikasjon.module.scss";

interface NotifikasjonProps {
  tittel: string;
  melding: string;
  href: string;
}

export function Notifikasjon({ tittel, melding, href }: NotifikasjonProps) {
  return (
    <>
      <LinkPanel
        href={href}
        className={styles.notifikasjon_container}
        border={false}
        data-testid="notifikasjoner"
      >
        <div className={styles.flex}>
          <div>
            <span className={styles.ikon_container}>
              <BellIcon className={styles.ikon} aria-label="Notifikasjoner" />
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
