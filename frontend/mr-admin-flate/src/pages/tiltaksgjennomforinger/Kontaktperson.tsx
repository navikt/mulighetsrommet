import styles from "../DetaljerInfo.module.scss";
import { TEAMS_DYPLENKE } from "mulighetsrommet-frontend-common/constants";
import { TiltaksgjennomforingKontaktperson } from "mulighetsrommet-api-client";
import { BodyShort } from "@navikt/ds-react";

interface Props {
  kontaktperson: TiltaksgjennomforingKontaktperson;
}

export function Kontaktperson({ kontaktperson }: Props) {
  return (
    <div className={styles.leverandor_kontaktinfo}>
      <BodyShort>{kontaktperson.navn}</BodyShort>
      {kontaktperson.beskrivelse && <BodyShort>{kontaktperson.beskrivelse}</BodyShort>}
      <BodyShort>{kontaktperson.mobilnummer}</BodyShort>
      <a href={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>{kontaktperson.epost}</a>
    </div>
  );
}
