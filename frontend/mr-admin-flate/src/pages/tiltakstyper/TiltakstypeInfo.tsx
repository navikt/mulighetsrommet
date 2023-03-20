import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Metadata } from "../../components/detaljside/Metadata";
import { Tiltakstypestatus } from "../../components/statuselementer/Tiltakstypestatus";
import { formaterDato } from "../../utils/Utils";
import styles from "./TiltakstypeInfo.module.scss";

export function TiltakstypeInfo() {
  const { data } = useTiltakstypeById();

  if (!data) {
    return null;
  }

  const tiltakstype = data;
  return (
    <div>
      <dl className={styles.detaljer}>
        <Metadata header="Tiltakstype" verdi={tiltakstype.navn} />
        <Metadata header="Tiltakskode" verdi={tiltakstype.arenaKode} />
        <Metadata
          header="Status"
          verdi={<Tiltakstypestatus tiltakstype={tiltakstype} />}
        />
        <div>&nbsp;</div>
        <Metadata
          header="Startdato"
          verdi={formaterDato(tiltakstype.fraDato)}
        />
        <Metadata
          header="Sluttdato"
          verdi={formaterDato(tiltakstype.tilDato)}
        />
      </dl>
    </div>
  );
}
