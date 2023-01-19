import { BodyShort } from "@navikt/ds-react";
import styles from "./TiltakstypeRad.module.scss";

export function ListeheaderTiltakstype() {
  return (
    <li className={styles.listeheader_wrapper}>
      <BodyShort size="medium">Tittel</BodyShort>
      <div className={styles.dato}>
        <BodyShort size="small">Startdato</BodyShort>
        <BodyShort size="small">Sluttdato</BodyShort>
      </div>
    </li>
  );
}
