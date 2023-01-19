import { BodyShort } from "@navikt/ds-react";
import styles from "./TiltaksgjennomforingRad.module.scss";

export function ListeheaderTiltaksgjennomforing() {
  return (
    <li className={styles.listeheader_wrapper}>
      <BodyShort size="medium">Tittel</BodyShort>
      <BodyShort size="medium">Tiltaksnummer</BodyShort>
      <BodyShort size="small">Tiltakstype</BodyShort>
      <BodyShort size="small">Enhet</BodyShort>
    </li>
  );
}
