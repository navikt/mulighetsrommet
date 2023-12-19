import { Alert, BodyShort } from "@navikt/ds-react";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { Laster } from "../laster/Laster";
import { TiltaksgjennomforingstatusTag } from "../statuselementer/TiltaksgjennomforingstatusTag";
import styles from "./TiltaksgjennomforingerListe.module.scss";
import { TiltaksgjennomforingFilter } from "../../api/atoms";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { ReactNode } from "react";

interface Props {
  filter: Partial<TiltaksgjennomforingFilter>;
  action: (gjennomforing: Tiltaksgjennomforing) => ReactNode;
}

export function TiltaksgjennomforingerListe(props: Props) {
  const { data, isError, isPending } = useAdminTiltaksgjennomforinger(props.filter);

  if (isError) {
    return <Alert variant="error">Vi hadde problemer med henting av tiltaksgjennomføringer</Alert>;
  }

  if (isPending) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  const tiltaksgjennomforinger = data.data;

  return (
    <div className={styles.gjennomforingsliste_container}>
      <div className={styles.gjennomforingsliste_headers}>
        <BodyShort>Tittel</BodyShort>
        <BodyShort>Tiltaksnr.</BodyShort>
        <BodyShort>Status</BodyShort>
      </div>

      <ul className={styles.gjennomforingsliste}>
        {tiltaksgjennomforinger.map((gjennomforing) => (
          <li key={gjennomforing.id} className={styles.gjennomforingsliste_element}>
            <BodyShort>{gjennomforing.navn}</BodyShort>
            <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
            <TiltaksgjennomforingstatusTag tiltaksgjennomforing={gjennomforing} />
            {props.action(gjennomforing)}
          </li>
        ))}
      </ul>
    </div>
  );
}
