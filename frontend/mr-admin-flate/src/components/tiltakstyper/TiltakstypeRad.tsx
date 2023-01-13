import { Tiltakstype } from "mulighetsrommet-api-client";
import { BodyShort } from "@navikt/ds-react";
import styles from "./Tiltakstyperad.module.scss";
import { Next } from "@navikt/ds-icons";
import { Link } from "react-router-dom";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  return (
    <li className={styles.rad}>
      <Link to={`${tiltakstype.id}`}>
        <BodyShort size="medium">{tiltakstype.navn}</BodyShort>
      </Link>
      <BodyShort size="small">{tiltakstype.arenaKode}</BodyShort>
      <Next className={styles.pil} />
    </li>
  );
}
