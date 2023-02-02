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
