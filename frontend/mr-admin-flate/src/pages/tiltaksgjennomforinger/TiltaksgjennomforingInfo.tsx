import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";

export function TiltaksgjennomforingInfo() {
  const { data } = useTiltaksgjennomforingById();

  if (!data) {
    return null;
  }

  const tiltaksgjennomforing = data;
  return (
    <div className={styles.detaljer}>
      <dl>
        <Metadata
          header="Tiltakstype"
          verdi={tiltaksgjennomforing.tiltakstype.navn}
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
