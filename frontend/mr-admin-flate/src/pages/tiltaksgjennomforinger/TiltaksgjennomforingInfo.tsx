import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import { Alert, Button } from "@navikt/ds-react";
import classNames from "classnames";
import { OpprettTiltaksgjennomforingModal } from "../../components/tiltaksgjennomforinger/opprett/OpprettTiltaksgjennomforingModal";
import { useState } from "react";
import { useFeatureToggles } from "../../api/features/feature-toggles";

export function TiltaksgjennomforingInfo() {
  const { data: tiltaksgjennomforing, isError, isLoading } = useTiltaksgjennomforingById();
  const { data: features } = useFeatureToggles();

  const [redigerModal, setRedigerModal] = useState(false);
  const handleRediger = () => setRedigerModal(true);
  const lukkRedigerModal = () => setRedigerModal(false);

  if (!tiltaksgjennomforing && isLoading) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Klarte ikke hente informasjon om tiltaksgjennomføring
      </Alert>
    );
  }

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">Fant ingen tiltaksgjennomføring</Alert>;
  }

  return (
    <div className={styles.container}>
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
      <div className={styles.knapperad}>
        {features?.["mulighetsrommet.admin-flate-rediger-avtale"] ? (
          <Button
            variant="tertiary"
            onClick={handleRediger}
            data-testid="endre-avtale"
          >
            Endre
          </Button>
        ) : null}
      </div>
      <OpprettTiltaksgjennomforingModal
        modalOpen={redigerModal}
        onClose={lukkRedigerModal}
        shouldCloseOnOverlayClick={true}
        tiltaksgjennomforing={tiltaksgjennomforing}
      />
    </div>
  );
}
