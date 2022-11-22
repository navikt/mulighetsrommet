import { Tiltaksgjennomforing } from "../../../mulighetsrommet-api-client";
import { BodyLong, BodyShort } from "@navikt/ds-react";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function Tiltaksgjennomforingrad({ tiltaksgjennomforing }: Props) {
  return (
    <li
      style={{
        display: "grid",
        gridTemplateColumns: "3fr 1fr 1fr 1fr 1fr",
        backgroundColor: "white",
        marginBottom: "1rem",
        padding: "2rem",
      }}
    >
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
