import { Tiltakstype } from "mulighetsrommet-api-client";
import { BodyLong, BodyShort } from "@navikt/ds-react";
import styles from "./Tiltakstyperad.module.scss";
import { Next } from "@navikt/ds-icons";
import { Link } from "react-router-dom";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstyperad({ tiltakstype }: Props) {
  return (
    <li className={styles.rad}>
      <Link to={`${tiltakstype.id}`}>
        <BodyLong size={"medium"}>{tiltakstype.navn}</BodyLong>
      </Link>
      <BodyShort size={"small"}>{tiltakstype.arenaKode}</BodyShort>
      <div className={styles.pil}>
        <Next />
      </div>
    </li>
  );
}
