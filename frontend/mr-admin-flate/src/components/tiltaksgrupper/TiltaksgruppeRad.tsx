import { Next } from "@navikt/ds-icons";
import { BodyShort } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import styles from "./TiltaksgruppeRad.module.scss";
import { Tiltaksgruppe } from "../../api/tiltaksgrupper/useTiltaksgrupper";

interface Props {
  tiltaksgruppe: Tiltaksgruppe;
}

export function TiltaksgruppeRad({ tiltaksgruppe }: Props) {
  return (
    <li className={styles.list_element}>
      <Link to={`/tiltaksgrupper/${tiltaksgruppe.id}`} className={styles.rad}>
        <BodyShort size="medium">{tiltaksgruppe.navn}</BodyShort>
        <BodyShort size="small">{tiltaksgruppe.arenaKode}</BodyShort>
        <Next className={styles.pil} />
      </Link>
    </li>
  );
}
