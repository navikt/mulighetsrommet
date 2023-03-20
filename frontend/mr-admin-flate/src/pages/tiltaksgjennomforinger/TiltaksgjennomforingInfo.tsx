import { Metadata } from "../../components/detaljside/Metadata";
import { formaterDato } from "../../utils/Utils";
import styles from "./TiltaksgjennomforingInfo.module.scss";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";

export function TiltaksgjennomforingInfo() {
  const { data } = useTiltaksgjennomforingById();

  if (!data) {
    return null;
  }

  const tiltaksgjennomforing = data;
  return (
    <div>
      <dl className={styles.detaljer}>
        <div className={styles.bolk}>
          <Metadata
            header="Tiltakstype"
            verdi={tiltaksgjennomforing.tiltakstype.navn}
          />
        </div>
        <div className={styles.bolk}>
          <Metadata
            header="Startdato"
            verdi={formaterDato(tiltaksgjennomforing.startDato)}
          />
          <Metadata
            header="Sluttdato"
            verdi={formaterDato(tiltaksgjennomforing.sluttDato)}
          />
        </div>

        <div className={styles.bolk}>
          <Metadata header="Enhet" verdi={tiltaksgjennomforing.enhet} />
        </div>

        {tiltaksgjennomforing.virksomhetsnavn ? (
          <div className={styles.bolk}>
            <Metadata
              header="Arrangør"
              verdi={tiltaksgjennomforing.virksomhetsnavn}
            />
          </div>
        ) : null}
      </dl>
    </div>
  );
}
