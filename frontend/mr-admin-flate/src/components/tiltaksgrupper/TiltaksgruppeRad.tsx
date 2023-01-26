import { BodyShort } from "@navikt/ds-react";
import styles from "../listeelementer/Listeelementer.module.scss";
import { Tiltaksgruppe } from "../../api/tiltaksgrupper/useTiltaksgrupper";
import { ListeRad } from "../listeelementer/ListeRad";

interface Props {
  tiltaksgruppe: Tiltaksgruppe;
}

export function TiltaksgruppeRad({ tiltaksgruppe }: Props) {
  return (
    <ListeRad
      linkTo={`/tiltaksgrupper/${tiltaksgruppe.id}`}
      classname={styles.listerad_tiltaksgruppe}
    >
      <BodyShort size="medium">{tiltaksgruppe.navn}</BodyShort>
      <BodyShort size="small">{tiltaksgruppe.arenaKode}</BodyShort>
    </ListeRad>
  );
}
