import { Tiltakstype } from "mulighetsrommet-api-client";
import { BodyLong, BodyShort } from "@navikt/ds-react";
import styles from "./Tiltakstyperad.module.scss";
import { Next } from "@navikt/ds-icons";
import { Link } from "react-router-dom";
import { formaterDato } from "../../utils/Utils";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstyperad({ tiltakstype }: Props) {
  return (
    <li className={styles.rad}>
      <Link to={`${tiltakstype.id}`}>
        <BodyLong size={"medium"}>{tiltakstype.navn}</BodyLong>
      </Link>
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
      <div className={styles.pil}>
        <Next />
      </div>
    </li>
  );
}
