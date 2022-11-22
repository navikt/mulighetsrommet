import { Tiltaksgjennomforing } from "../../../mulighetsrommet-api-client";
import { BodyLong, BodyShort } from "@navikt/ds-react";
import styles from "./Tiltaksgjennomforing.module.scss";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function Tiltaksgjennomforingrad({ tiltaksgjennomforing }: Props) {
  return (
    <li className={styles.rad}>
      <BodyLong size={"medium"}>{tiltaksgjennomforing.navn}</BodyLong>
      <BodyShort size={"small"}>{tiltaksgjennomforing.tiltakskode}</BodyShort>
      <BodyShort size={"small"}>{tiltaksgjennomforing.tiltaksnummer}</BodyShort>
      <BodyShort size={"small"}>
        {tiltaksgjennomforing.tilgjenglighet}
      </BodyShort>
      <BodyShort size={"small"}>{tiltaksgjennomforing.aar}</BodyShort>
    </li>
  );
}
