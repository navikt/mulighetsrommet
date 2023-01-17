import { Tiltakstype } from "mulighetsrommet-api-client";
import { BodyShort } from "@navikt/ds-react";
import styles from "./TiltakstypeRad.module.scss";
import { Next } from "@navikt/ds-icons";
import { formaterDato } from "../../utils/Utils";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  const navigerTilTiltakstype = () =>
    (location.href = `/tiltakstyper/${tiltakstype.id}`);
  return (
    <li className={styles.rad} onClick={navigerTilTiltakstype}>
      <BodyShort size="medium">{tiltakstype.navn}</BodyShort>
      <BodyShort size="small">{tiltakstype.arenaKode}</BodyShort>
      <div className={styles.dato}>
        <BodyShort
          size="small"
          title={`Startdato ${formaterDato(tiltakstype.fraDato)}`}
        >
          {formaterDato(tiltakstype.fraDato)}
        </BodyShort>
        <BodyShort
          size="small"
          title={`Sluttdato ${formaterDato(tiltakstype.tilDato)}`}
        >
          {formaterDato(tiltakstype.tilDato)}
        </BodyShort>
      </div>
      <Next className={styles.pil} />
    </li>
  );
}
