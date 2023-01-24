import styles from "./Listeelementer.module.scss";
import classNames from "classnames";
import { BodyShort } from "@navikt/ds-react";

interface ListeheaderProps {
  children: any;
  classname?: string;
}

function Listeheader({ children, classname }: ListeheaderProps) {
  return (
    <li className={classNames(styles.listeheader, classname)}>{children}</li>
  );
}

export function ListeheaderTiltaksgjennomforinger() {
  return (
    <Listeheader classname={styles.listeheader_tiltaksgjennomforinger}>
      <BodyShort size="medium">Tittel</BodyShort>
      <BodyShort size="medium">Tiltaksnummer</BodyShort>
      <BodyShort size="small">Tiltakstype</BodyShort>
      <BodyShort size="small">Enhet</BodyShort>
    </Listeheader>
  );
}

export function ListeheaderTiltakstyper() {
  return (
    <Listeheader classname={styles.listeheader_tiltakstyper}>
      <BodyShort size="medium">Tittel</BodyShort>
      <div className={styles.dato}>
        <BodyShort size="small">Startdato</BodyShort>
        <BodyShort size="small">Sluttdato</BodyShort>
      </div>
    </Listeheader>
  );
}

export function ListeheaderTiltaksgrupper() {
  return (
    <Listeheader classname={styles.listeheader_tiltaksgrupper}>
      <BodyShort size="medium">Tittel</BodyShort>
      <BodyShort size="small">Arenakode</BodyShort>
    </Listeheader>
  );
}
