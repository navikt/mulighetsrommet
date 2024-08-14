import { Alert, BodyShort } from "@navikt/ds-react";
import { useAdminTiltaksgjennomforinger } from "@/api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { Laster } from "../laster/Laster";
import styles from "./TiltaksgjennomforingerListe.module.scss";
import { TiltaksgjennomforingFilter } from "@/api/atoms";
import { Tiltaksgjennomforing } from "@mr/api-client";
import { ReactNode } from "react";
import { TiltaksgjennomforingStatusTag } from "@mr/frontend-common";

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
            <TiltaksgjennomforingStatusTag status={gjennomforing.status} />
            {props.action(gjennomforing)}
          </li>
        ))}
      </ul>
    </div>
  );
}
