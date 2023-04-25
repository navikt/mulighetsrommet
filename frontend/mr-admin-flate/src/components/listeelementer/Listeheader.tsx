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

export function ListeheaderTiltakstyper() {
  return (
    <Listeheader classname={styles.listeheader_tiltakstyper}>
      <BodyShort>Tittel</BodyShort>
      <BodyShort>Status</BodyShort>
      <BodyShort>Startdato</BodyShort>
      <BodyShort>Sluttdato</BodyShort>
    </Listeheader>
  );
}

export function ListeheaderTiltaksgjennomforinger() {
  return (
    <Listeheader classname={styles.listeheader_tiltaksgjennomforinger}>
      <BodyShort>Tittel</BodyShort>
      <BodyShort>Tiltaksnr.</BodyShort>
      <BodyShort>Arrangør</BodyShort>
      <BodyShort>Tiltakstype</BodyShort>
      <BodyShort>Startdato</BodyShort>
      <BodyShort>Sluttdato</BodyShort>
      <BodyShort>Status</BodyShort>
    </Listeheader>
  );
}
