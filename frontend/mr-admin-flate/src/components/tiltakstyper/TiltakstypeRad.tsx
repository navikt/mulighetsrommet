import { BodyShort } from "@navikt/ds-react";
import { Tiltakstype } from "mulighetsrommet-api-client";
import { formaterDato } from "../../utils/Utils";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeRad } from "../listeelementer/ListeRad";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  return (
    <ListeRad
      linkTo={`/tiltakstyper/${tiltakstype.id}`}
      classname={styles.listerad_tiltakstype}
    >
      <BodyShort size="medium">{tiltakstype.navn}</BodyShort>
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
    </ListeRad>
  );
}
