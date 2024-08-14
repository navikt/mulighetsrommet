import { Alert, BodyShort } from "@navikt/ds-react";
import { Laster } from "../laster/Laster";
import styles from "../tiltaksgjennomforinger/TiltaksgjennomforingerListe.module.scss";
import { AvtaleFilter } from "@/api/atoms";
import { Avtale } from "@mr/api-client";
import { ReactNode } from "react";
import { useAvtaler } from "@/api/avtaler/useAvtaler";
import { AvtalestatusTag } from "../statuselementer/AvtalestatusTag";

interface Props {
  filter: Partial<AvtaleFilter>;
  action: (avtale: Avtale) => ReactNode;
}

export function AvtaleListe(props: Props) {
  const { data, isError, isPending } = useAvtaler(props.filter);

  if (isError) {
    return <Alert variant="error">Vi hadde problemer med å hente avtaler</Alert>;
  }

  if (isPending) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  const avtaler = data.data;

  return (
    <div className={styles.gjennomforingsliste_container}>
      <div className={styles.gjennomforingsliste_headers}>
        <BodyShort>Tittel</BodyShort>
        <BodyShort>Avtalenummer</BodyShort>
        <BodyShort>Status</BodyShort>
      </div>

      <ul className={styles.gjennomforingsliste}>
        {avtaler.map((avtale) => (
          <li key={avtale.id} className={styles.gjennomforingsliste_element}>
            <BodyShort>{avtale.navn}</BodyShort>
            <BodyShort>{avtale.avtalenummer}</BodyShort>
            <AvtalestatusTag avtale={avtale} />
            {props.action(avtale)}
          </li>
        ))}
      </ul>
    </div>
  );
}
