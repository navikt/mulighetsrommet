import { Next } from "@navikt/ds-icons";
import { BodyShort } from "@navikt/ds-react";
import { Tiltakstype } from "mulighetsrommet-api-client";
import { Link } from "react-router-dom";
import { formaterDato } from "../../utils/Utils";
import styles from "./TiltakstypeRad.module.scss";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  return (
    <li className={styles.list_element}>
      <Link to={`/tiltakstyper/${tiltakstype.id}`} className={styles.rad}>
        <BodyShort size="medium">{tiltakstype.navn}</BodyShort>
        <BodyShort size="small">{tiltakstype.arenaKode}</BodyShort>
        <div className={styles.dato}>
          <BodyShort
            size="small"
            title={`Startdato ${formaterDato(tiltakstype.fraDato)}`}
          >
            {formaterDato(tiltakstype.fraDato)}
          </BodyShort>
          <BodyShort
            size="small"
            title={`Sluttdato ${formaterDato(tiltakstype.tilDato)}`}
          >
            {formaterDato(tiltakstype.tilDato)}
          </BodyShort>
        </div>
        <Next className={styles.pil} />
      </Link>
    </li>
  );
}
