import { Tiltakstype } from "mulighetsrommet-api-client";
import { Tiltakstypestatus } from "../../components/statuselementer/Tiltakstypestatus";
import { formaterDato } from "../../utils/Utils";
import styles from "./Tiltakstypedetaljer.module.scss";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeDetaljer({ tiltakstype }: Props) {
  return (
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
  );
}
