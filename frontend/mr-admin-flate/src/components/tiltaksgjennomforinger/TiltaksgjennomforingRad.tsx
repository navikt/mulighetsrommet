import { Next } from "@navikt/ds-icons";
import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { Link } from "react-router-dom";
import { useSideForNavigering } from "../../hooks/useSideForNavigering";
import styles from "./Tiltaksgjennomforing.module.scss";

interface TiltaksgjennomforingRadProps {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  fagansvarlig?: boolean;
}

export function TiltaksgjennomforingRad({
  tiltaksgjennomforing,
  fagansvarlig = false,
}: TiltaksgjennomforingRadProps) {
  const side = useSideForNavigering();

  const navigerSomFagEllerTiltaksansvarlig = () => {
    return fagansvarlig
      ? `${side}/${tiltaksgjennomforing.tiltakstype.id}/tiltaksgjennomforing/${tiltaksgjennomforing.id}`
      : `${side}/tiltaksgjennomforing/${tiltaksgjennomforing.id}`;
  };

  return (
    <li
      className={styles.rad}
      onClick={navigerSomFagEllerTiltaksansvarlig}
      data-testid="tiltaksgjennomforingsrad"
    >
      <Link to={navigerSomFagEllerTiltaksansvarlig()}>
        <BodyShort>{tiltaksgjennomforing.navn}</BodyShort>
      </Link>
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
      <Next className={styles.pil} />
    </li>
  );
}
