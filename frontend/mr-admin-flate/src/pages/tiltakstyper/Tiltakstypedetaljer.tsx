import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Tiltakstypestatus } from "../../components/statuselementer/Tiltakstypestatus";
import { formaterDato } from "../../utils/Utils";
import styles from "./Tiltakstypedetaljer.module.scss";

export function TiltakstypeDetaljer() {
  const { data } = useTiltakstypeById();

  if (!data) {
    return null;
  }

  const tiltakstype = data;
  return (
    <div>
      <dl className={styles.detaljer}>
        <dt>Tiltakstype</dt>
        <dd>{tiltakstype.navn}</dd>
        <dt>Tiltakskode</dt>
        <dd>{tiltakstype.arenaKode}</dd>
        <dt>Status</dt>
        <dd>
          <Tiltakstypestatus tiltakstype={tiltakstype} />
        </dd>
        <dt>Start</dt>
        <dd>{formaterDato(tiltakstype.fraDato)}</dd>
        <dt>Slutt</dt>
        <dd>{formaterDato(tiltakstype.tilDato)}</dd>
      </dl>
    </div>
  );
}
