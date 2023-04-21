import { BodyShort } from "@navikt/ds-react";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { hentListeMedEnhetsnavn } from "../../utils/TiltaksgjennomforingUtils";
import { formaterDato } from "../../utils/Utils";
import { ListeRad } from "../listeelementer/ListeRad";
import styles from "../listeelementer/Listeelementer.module.scss";
import { Tiltaksgjennomforingstatus } from "../statuselementer/Tiltaksgjennomforingstatus";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingsRad({ tiltaksgjennomforing }: Props) {
  const { data: enheter } = useAlleEnheter();
  const enhetsNavn = hentListeMedEnhetsnavn(
    enheter,
    tiltaksgjennomforing.enheter
  );

  const formaterEnheter = (enheter: string[]): string => {
    return enheter.length > 1
      ? `${enhetsNavn.shift()} + ${enhetsNavn.length}`
      : enhetsNavn[0];
  };

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
      <BodyShort aria-label={`Enheter for tiltaksgjennomforing: ${enhetsNavn}`}>
        {formaterEnheter(enhetsNavn)}
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
        title={`Sluttdato ${formaterDato(tiltaksgjennomforing.sluttDato)}, "-"`}
        aria-label={
          tiltaksgjennomforing.sluttDato
            ? `Sluttdato: ${formaterDato(tiltaksgjennomforing.sluttDato, "-")}`
            : undefined // Noen gjennomføringer har ikke sluttdato så da setter vi heller ikke aria-label for da klager reactA11y
        }
      >
        {formaterDato(tiltaksgjennomforing.sluttDato)}
      </BodyShort>
      <BodyShort>
        <Tiltaksgjennomforingstatus
          tiltaksgjennomforing={tiltaksgjennomforing}
        />
      </BodyShort>
    </ListeRad>
  );
}
