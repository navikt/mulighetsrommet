import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./Tiltaksgjennomforing.module.scss";
import { Next } from "@navikt/ds-icons";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  fagansvarlig?: boolean;
}

export function TiltaksgjennomforingRad({
  tiltaksgjennomforing,
  fagansvarlig = false,
}: Props) {
  return (
    <li
      className={styles.rad}
      onClick={() =>
        (location.href = fagansvarlig
          ? `/tiltaksgjennomforing/${tiltaksgjennomforing.id}`
          : `/${tiltaksgjennomforing.id}`)
      }
    >
      <BodyShort>{tiltaksgjennomforing.navn}</BodyShort>
      <BodyShort size="small">{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort
        size="small"
        title={tiltaksgjennomforing.tiltakstype.arenaKode}
      >
        {tiltaksgjennomforing.tiltakstype.navn}
      </BodyShort>
      <BodyShort size="small">
        {tiltaksgjennomforing.virksomhetsnummer}
      </BodyShort>
      {/*<div className={styles.pil}>*/}
      <Next className={styles.pil} />
      {/*</div>*/}
    </li>
  );
}
