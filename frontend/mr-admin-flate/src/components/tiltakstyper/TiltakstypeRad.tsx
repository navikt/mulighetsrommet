import { Tiltakstype } from "mulighetsrommet-api-client";
import { BodyShort } from "@navikt/ds-react";
import styles from "./TiltakstypeRad.module.scss";
import { Next } from "@navikt/ds-icons";
import { formaterDato } from "../../utils/Utils";
import { Link } from "react-router-dom";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  return (
    <li className={styles.rad}>
      <Link to={`/tiltakstyper/${tiltakstype.id}`}>
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
