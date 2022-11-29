import { Tiltaksgjennomforing } from "../../../../mulighetsrommet-api-client";
import { BodyLong, BodyShort } from "@navikt/ds-react";
import styles from "./Tiltaksgjennomforing.module.scss";
import { Next } from "@navikt/ds-icons";
import { Link } from "react-router-dom";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function Tiltaksgjennomforingrad({ tiltaksgjennomforing }: Props) {
  return (
    <li className={styles.rad}>
      <Link to={`${tiltaksgjennomforing.id}`}>
        <BodyLong size={"medium"}>{tiltaksgjennomforing.navn}</BodyLong>
      </Link>
      <BodyShort size={"small"}>{tiltaksgjennomforing.tiltakskode}</BodyShort>
      <BodyShort size={"small"}>{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort size={"small"}>
        {tiltaksgjennomforing.tilgjengelighet}
      </BodyShort>
      <BodyShort size={"small"}>{tiltaksgjennomforing.aar}</BodyShort>
      <div className={styles.pil}>
        <Next />
      </div>
    </li>
  );
}
