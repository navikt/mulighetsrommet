import { Next } from "@navikt/ds-icons";
import { BodyLong, BodyShort } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { TiltaksgjennomforingMedTiltakstype } from "../../../../mulighetsrommet-api-client";
import styles from "./Tiltaksgjennomforing.module.scss";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingMedTiltakstype;
  fagansvarlig?: boolean;
}

export function Tiltaksgjennomforingrad({
  tiltaksgjennomforing,
  fagansvarlig = false,
}: Props) {
  return (
    <li className={styles.rad}>
      <Link
        to={
          fagansvarlig
            ? `/tiltaksgjennomforing/${tiltaksgjennomforing.id}`
            : `/${tiltaksgjennomforing.id}`
        }
      >
        <BodyLong>{tiltaksgjennomforing.navn}</BodyLong>
      </Link>
      <BodyShort size="small">{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort size="small" title={tiltaksgjennomforing.tiltakskode}>
        {tiltaksgjennomforing.tiltakstypeNavn}
      </BodyShort>
      <BodyShort size="small">
        {tiltaksgjennomforing.virksomhetsnummer}
      </BodyShort>
      <div className={styles.pil}>
        <Next />
      </div>
    </li>
  );
}
