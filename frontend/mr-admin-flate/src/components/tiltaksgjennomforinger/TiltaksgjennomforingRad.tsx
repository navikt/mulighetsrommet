import { Next } from "@navikt/ds-icons";
import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./TiltaksgjennomforingRad.module.scss";
import { Link } from "react-router-dom";
import { useSideForNavigering } from "../../hooks/useSideForNavigering";

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
          className={styles.list_element}
          onClick={navigerSomFagEllerTiltaksansvarlig}
          data-testid="tiltaksgjennomforingsrad"
      >
          <Link to={navigerSomFagEllerTiltaksansvarlig()} className={styles.rad}>
              <BodyShort>{tiltaksgjennomforing.navn}</BodyShort>
              <BodyShort size="small">{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
              <BodyShort size="small">
                  {tiltaksgjennomforing.tiltakstype.navn}
              </BodyShort>
              <BodyShort size="small">{tiltaksgjennomforing.enhet}</BodyShort>
              <Next className={styles.pil} />
          </Link>
      </li>
  );
}
