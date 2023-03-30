import classNames from "classnames";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Tiltakstypestatus } from "../../components/statuselementer/Tiltakstypestatus";
import { formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";

export function TiltakstypeInfo() {
  const { data } = useTiltakstypeById();

  if (!data) {
    return null;
  }

  const tiltakstype = data;
  return (
    <div className={classNames(styles.detaljer, styles.container)}>
      <div className={styles.bolk}>
        <Metadata header="Tiltakstype" verdi={tiltakstype.navn} />
        <Metadata header="Tiltakskode" verdi={tiltakstype.arenaKode} />
      </div>
      <Separator />
      <Metadata
        header="Status"
        verdi={<Tiltakstypestatus tiltakstype={tiltakstype} />}
      />
      <Separator />
      <div className={styles.bolk}>
        <Metadata
          header="Startdato"
          verdi={formaterDato(tiltakstype.fraDato)}
        />
        <Metadata
          header="Sluttdato"
          verdi={formaterDato(tiltakstype.tilDato)}
        />
      </div>
    </div>
  );
}
