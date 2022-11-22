import { Tiltaksgjennomforing } from "../../../../mulighetsrommet-api-client";
import { Link, BodyLong, BodyShort } from "@navikt/ds-react";
import styles from "./Tiltaksgjennomforing.module.scss";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function Tiltaksgjennomforingrad({ tiltaksgjennomforing }: Props) {
  return (
    <li className={styles.rad}>
      <Link>
        <BodyLong size={"medium"}>{tiltaksgjennomforing.navn}</BodyLong>
      </Link>
      <BodyShort size={"small"}>{tiltaksgjennomforing.tiltakskode}</BodyShort>
      <BodyShort size={"small"}>{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort size={"small"}>
        {tiltaksgjennomforing.tilgjenglighet}
      </BodyShort>
      <BodyShort size={"small"}>{tiltaksgjennomforing.aar}</BodyShort>
    </li>
  );
}
