import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useSideForNavigering } from "../../hooks/useSideForNavigering";
import { ListeRad } from "../listeelementer/ListeRad";
import styles from "../listeelementer/Listeelementer.module.scss";

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
    <ListeRad
      linkTo={navigerSomFagEllerTiltaksansvarlig()}
      classname={styles.listerad_tiltaksgjennomforing}
    >
      <BodyShort>{tiltaksgjennomforing.navn}</BodyShort>
      <BodyShort size="small">{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort size="small">
        {tiltaksgjennomforing.tiltakstype.navn}
      </BodyShort>
      <BodyShort size="small">{tiltaksgjennomforing.enhet}</BodyShort>
    </ListeRad>
  );
}
