import styles from "../DetaljerInfo.module.scss";
import { TEAMS_DYPLENKE } from "mulighetsrommet-frontend-common/constants";
import { TiltaksgjennomforingKontaktpersoner } from "mulighetsrommet-api-client";

interface Props {
  kontaktperson: TiltaksgjennomforingKontaktpersoner;
}

export function Kontaktperson({ kontaktperson }: Props) {
  return (
    <div className={styles.leverandor_kontaktinfo}>
      <label>{kontaktperson.navn}</label>
      <label>{kontaktperson.mobilnummer}</label>
      <a href={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>{kontaktperson.epost}</a>
    </div>
  );
}
