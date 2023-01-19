import { Next } from "@navikt/ds-icons";
import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./TiltaksgjennomforingRad.module.scss";
import { useSideForNavigering } from "../../hooks/useSideForNavigering";

interface TiltaksgjennomforingRadProps {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  fagansvarlig?: boolean;
}

export function TiltaksgjennomforingRad({
  tiltaksgjennomforing,
  fagansvarlig = false,
}: TiltaksgjennomforingRadProps) {
  const navigerSomFagEllerTiltaksansvarlig = () => {
    location.href = fagansvarlig
      ? `${side}/${tiltaksgjennomforing.tiltakstype.id}/tiltaksgjennomforing/${tiltaksgjennomforing.id}`
      : `${side}/tiltaksgjennomforing/${tiltaksgjennomforing.id}`;
  };
  const side = useSideForNavigering();

  return (
    <li
      className={styles.rad}
      onClick={navigerSomFagEllerTiltaksansvarlig}
      data-testid="tiltaksgjennomforingsrad"
    >
      <BodyShort>{tiltaksgjennomforing.navn}</BodyShort>
      <BodyShort size="small">{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort size="small">
        {tiltaksgjennomforing.tiltakstype.navn}
      </BodyShort>
      <BodyShort size="small">{tiltaksgjennomforing.enhet}</BodyShort>
      <Next className={styles.pil} />
    </li>
  );
}
