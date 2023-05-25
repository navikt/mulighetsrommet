import { Alert, BodyShort, Button } from "@navikt/ds-react";
import { Laster } from "../laster/Laster";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { Tiltaksgjennomforingstatus } from "../statuselementer/Tiltaksgjennomforingstatus";
import React from "react";
import { PlusIcon } from "@navikt/aksel-icons";
import styles from "./Tiltaksgjennomforingsliste.module.scss";

export const Tiltaksgjennomforingsliste = () => {
  const { data, isLoading, isError } = useAdminTiltaksgjennomforinger();
  const tiltaksgjennomforinger = data?.data ?? [];

  if (
    (!tiltaksgjennomforinger || tiltaksgjennomforinger.length === 0) &&
    isLoading
  ) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  if (tiltaksgjennomforinger.length === 0) {
    return <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltaksgjennomføringer
      </Alert>
    );
  }

  if (tiltaksgjennomforinger.length === 0) {
    return (
      <Alert variant="info">
        Det finnes ingen tiltaksgjennomføringer for avtalen.
      </Alert>
    );
  }

  return (
    <div className={styles.gjennomforingsliste_container}>
      <div className={styles.gjennomforingsliste_headers}>
        <BodyShort>Tittel</BodyShort>
        <BodyShort>Tiltaksnr.</BodyShort>
        <BodyShort>Status</BodyShort>
      </div>

      {tiltaksgjennomforinger.map((gjennomforing, index) => (
        <div key={index} className={styles.gjennomforingsliste}>
          <BodyShort>{gjennomforing.navn}</BodyShort>
          <BodyShort>{gjennomforing.tiltaksnummer}</BodyShort>
          <Tiltaksgjennomforingstatus tiltaksgjennomforing={gjennomforing} />
          <Button variant="tertiary" className={styles.legg_til_knapp}>
            <PlusIcon fontSize={22} />
            Legg til
          </Button>
        </div>
      ))}
    </div>
  );
};
