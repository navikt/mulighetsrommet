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
        <Metadata
          header="Tiltaksgjennomforing"
          verdi={tiltaksgjennomforing.navn}
        />
        {/*<Metadata header="Tiltakskode" verdi={tiltaksgjennomforing.arenaKode} />*/}
        {/*<Metadata*/}
        {/*  header="Status"*/}
        {/*  verdi={<Tiltakstypestatus tiltakstype={tiltaksgjennomforing} />}*/}
        {/*/>*/}
        <div>&nbsp;</div>
        <Metadata
          header="Startdato"
          verdi={formaterDato(tiltaksgjennomforing.startDato)}
        />
        <Metadata
          header="Sluttdato"
          verdi={formaterDato(tiltaksgjennomforing.sluttDato)}
        />
      </dl>
    </div>
  );
}
