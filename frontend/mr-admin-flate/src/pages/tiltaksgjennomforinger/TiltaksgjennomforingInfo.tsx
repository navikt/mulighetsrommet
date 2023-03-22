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
    <dl className={styles.detaljer}>
      <Metadata
        header="Tiltakstype"
        verdi={tiltaksgjennomforing.tiltakstype.navn}
      />
      <Separator />
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
      <Separator />
      <div className={styles.bolk}>
        <Metadata header="Enhet" verdi={tiltaksgjennomforing.enhet} />
        {tiltaksgjennomforing.virksomhetsnavn ? (
          <Metadata
            header="Arrangør"
            verdi={tiltaksgjennomforing.virksomhetsnavn}
          />
        ) : null}
      </div>
    </dl>
  );
}
