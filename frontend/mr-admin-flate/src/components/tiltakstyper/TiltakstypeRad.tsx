import { BodyShort } from "@navikt/ds-react";
import { Tiltakstype } from "mulighetsrommet-api-client";
import { formaterDato } from "../../utils/Utils";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeRad } from "../listeelementer/ListeRad";
import { Tiltakstypestatus } from "../statuselementer/Tiltakstypestatus";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  return (
    <ListeRad
      linkTo={`/tiltakstyper/${tiltakstype.id}`}
      classname={styles.listerad_tiltakstype}
      testId="tiltakstyperad"
    >
      <BodyShort size="medium">{tiltakstype.navn}</BodyShort>
      <BodyShort size="medium">
        <Tiltakstypestatus tiltakstype={tiltakstype} />
      </BodyShort>
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
    </ListeRad>
  );
}
