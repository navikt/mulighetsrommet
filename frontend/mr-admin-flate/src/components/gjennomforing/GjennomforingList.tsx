import { Alert, BodyShort } from "@navikt/ds-react";
import { Laster } from "../laster/Laster";
import styles from "./GjennomforingList.module.scss";
import { GjennomforingFilter } from "@/api/atoms";
import { TiltaksgjennomforingDto } from "@mr/api-client";
import { ReactNode } from "react";
import { GjennomforingStatusTag } from "@mr/frontend-common";
import { useAdminGjennomforinger } from "@/api/gjennomforing/useAdminGjennomforinger";

interface Props {
  filter: Partial<GjennomforingFilter>;
  action: (gjennomforing: TiltaksgjennomforingDto) => ReactNode;
}

export function GjennomforingList(props: Props) {
  const { data, isError, isPending } = useAdminGjennomforinger(props.filter);

  if (isError) {
    return <Alert variant="error">Vi hadde problemer med henting av tiltaksgjennomføringer</Alert>;
  }

  if (isPending) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  const gjennomforinger = data.data;

  return (
    <div className={styles.gjennomforingsliste_container}>
      <div className={styles.gjennomforingsliste_headers}>
        <BodyShort>Tittel</BodyShort>
        <BodyShort>Tiltaksnr.</BodyShort>
        <BodyShort>Status</BodyShort>
      </div>

      <ul className={styles.gjennomforingsliste}>
        {gjennomforinger.map((gjennomforing) => (
          <li key={gjennomforing.id} className={styles.gjennomforingsliste_element}>
            <BodyShort>{gjennomforing.navn}</BodyShort>
            <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
            <GjennomforingStatusTag status={gjennomforing.status.status} />
            {props.action(gjennomforing)}
          </li>
        ))}
      </ul>
    </div>
  );
}
