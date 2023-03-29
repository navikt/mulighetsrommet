import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import { Alert } from "@navikt/ds-react";
import classNames from "classnames";

export function TiltaksgjennomforingInfo() {
  const { data, isError, isLoading } = useTiltaksgjennomforingById();

  if (!data && isLoading) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Klarte ikke hente informasjon om tiltaksgjennomføring
      </Alert>
    );
  }

  if (!data) {
    return <Alert variant="warning">Fant ingen tiltaksgjennomføring</Alert>;
  }

  const tiltaksgjennomforing = data;
  return (
    <div className={classNames(styles.detaljer, styles.container)}>
      <dl className={styles.bolk}>
        <Metadata
          header="Tiltakstype"
          verdi={tiltaksgjennomforing.tiltakstype.navn}
        />
        <Metadata
          header="Tiltaksnummer"
          verdi={tiltaksgjennomforing.tiltaksnummer}
        />
      </dl>
      <Separator />
      <dl className={styles.bolk}>
        <Metadata
          header="Startdato"
          verdi={formaterDato(tiltaksgjennomforing.startDato)}
        />
        <Metadata
          header="Sluttdato"
          verdi={formaterDato(tiltaksgjennomforing.sluttDato)}
        />
      </dl>
      <Separator />
      <dl className={styles.bolk}>
        <Metadata header="Enhet" verdi={tiltaksgjennomforing.enhet} />
        {tiltaksgjennomforing.virksomhetsnavn ? (
          <Metadata
            header="Arrangør"
            verdi={tiltaksgjennomforing.virksomhetsnavn}
          />
        ) : null}
      </dl>
    </div>
  );
}
