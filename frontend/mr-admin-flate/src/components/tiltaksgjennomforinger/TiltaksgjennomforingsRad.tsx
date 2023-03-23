import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { formaterDato } from "../../utils/Utils";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeRad } from "../listeelementer/ListeRad";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingsRad({ tiltaksgjennomforing }: Props) {
  return (
    <ListeRad
      linkTo={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}
      classname={styles.listerad_tiltaksgjennomforing}
      testId="tiltaksgjennomforingrad"
    >
      <BodyShort
        aria-label={`Navn på tiltaksgjennomforing: ${tiltaksgjennomforing.navn}`}
      >
        {tiltaksgjennomforing.navn}
      </BodyShort>
      <BodyShort
        aria-label={`Tiltaksnummer: ${tiltaksgjennomforing.tiltaksnummer}`}
      >
        {tiltaksgjennomforing.tiltaksnummer}
      </BodyShort>

      <BodyShort
        aria-label={`Virksomhetsnavn: ${tiltaksgjennomforing.virksomhetsnavn}`}
      >
        {tiltaksgjennomforing.virksomhetsnavn}
      </BodyShort>
      <BodyShort
        aria-label={`Tiltakstypenavn: ${tiltaksgjennomforing.tiltakstype.navn}`}
      >
        {tiltaksgjennomforing.tiltakstype.navn}
      </BodyShort>

      <BodyShort
        title={`Startdato ${formaterDato(tiltaksgjennomforing.startDato)}`}
        aria-label={`Startdato: ${formaterDato(
          tiltaksgjennomforing.startDato
        )}`}
      >
        {formaterDato(tiltaksgjennomforing.startDato)}
      </BodyShort>
      <BodyShort
        title={`Sluttdato ${formaterDato(tiltaksgjennomforing.sluttDato)}`}
        aria-label={`Sluttdato: ${formaterDato(
          tiltaksgjennomforing.sluttDato
        )}`}
      >
        {formaterDato(tiltaksgjennomforing.sluttDato)}
      </BodyShort>
    </ListeRad>
  );
}
