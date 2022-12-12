import { Tiltaksgjennomforing } from "../../../../mulighetsrommet-api-client";
import { BodyLong, BodyShort } from "@navikt/ds-react";
import styles from "./Tiltaksgjennomforing.module.scss";
import { Next } from "@navikt/ds-icons";
import { Link } from "react-router-dom";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
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
            : `${tiltaksgjennomforing.id}`
        }
      >
        <BodyLong>{tiltaksgjennomforing.navn}</BodyLong>
      </Link>
      <BodyShort size="small">{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      {/*TODO: Hente navn på tiltakstype her*/}
      <BodyShort size="small">{tiltaksgjennomforing.tiltakstypeId}</BodyShort>
      <BodyShort size="small">
        {tiltaksgjennomforing.virksomhetsnummer}
      </BodyShort>
      <div className={styles.pil}>
        <Next />
      </div>
    </li>
  );
}
