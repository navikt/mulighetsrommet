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
      <BodyShort size="medium">Tittel</BodyShort>
      <BodyShort size="medium">Status</BodyShort>
      <BodyShort size="small">Startdato</BodyShort>
      <BodyShort size="small">Sluttdato</BodyShort>
    </Listeheader>
  );
}

export function ListeheaderAvtaler() {
  return (
    <Listeheader classname={styles.listeheader_avtaler}>
      <BodyShort size="medium">Tittel</BodyShort>
      <BodyShort size="medium">Leverandør</BodyShort>
      <BodyShort size="medium">Enhet</BodyShort>
      <BodyShort size="medium">Dato fra</BodyShort>
      <BodyShort size="medium">Dato til</BodyShort>
      <BodyShort size="medium">Status</BodyShort>
    </Listeheader>
  );
}
