import { Tiltakstype } from "mulighetsrommet-api-client";
import { BodyShort } from "@navikt/ds-react";
import styles from "./Tiltakstyperad.module.scss";
import { Next } from "@navikt/ds-icons";

interface Props {
  tiltakstype: Tiltakstype;
}

export function Tiltakstyperad({ tiltakstype }: Props) {
  return (
    <li
      className={styles.rad}
      onClick={() => (location.href = `/tiltakstyper/${tiltakstype.id}`)}
    >
      <BodyShort size={"medium"}>{tiltakstype.navn}</BodyShort>
      <BodyShort size={"small"}>{tiltakstype.arenaKode}</BodyShort>
      <Next className={styles.pil} />
    </li>
  );
}
