import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import { Link } from "react-router-dom";
import { Avtale } from "mulighetsrommet-api-client";
import { BodyShort } from "@navikt/ds-react";

const avtaleFinnesIkke = () => (
  <>
    <BodyShort>
      Det finnes ingen avtale koblet til tiltaksgjennomføringen. Hvis gjennomføringen er en AFT-
      eller VTA-gjennomføring kan du koble gjennomføringen til riktig avtale. Gå til{" "}
      <Link to={`/avtaler`}>avtaler</Link>, finn riktig avtale og trykk «Legg til gjennomføring».
    </BodyShort>
    <BodyShort>
      Ta <a href={PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
    </BodyShort>
  </>
);

export const avtalenErAvsluttet = (erRedigeringsmodus: boolean) => (
  <>
    <BodyShort>
      Kan ikke {erRedigeringsmodus ? "redigere" : "opprette"} gjennomføring fordi avtalens sluttdato
      har passert.
    </BodyShort>
    <BodyShort>
      Ta <a href={PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
    </BodyShort>
  </>
);

const avtaleManglerNavRegionError = (avtaleId?: string) => (
  <>
    <BodyShort>
      Avtalen mangler NAV region. Du må oppdatere avtalens NAV region for å kunne opprette en
      gjennomføring.
      {avtaleId ? (
        <Link reloadDocument to={`/avtaler/${avtaleId}`}>
          Klikk her for å fikse avtalen
        </Link>
      ) : null}
    </BodyShort>
    <BodyShort>
      Ta <a href={PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
    </BodyShort>
  </>
);

export const tekniskFeilError = () => (
  <>
    <BodyShort>Gjennomføringen kunne ikke opprettes på grunn av en teknisk feil hos oss.</BodyShort>
    <BodyShort>
      Forsøk på nytt, eller ta <a href={PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
    </BodyShort>
  </>
);

export function ErrorMeldinger(avtale: Avtale | undefined, redigeringsModus: boolean | undefined) {
  if (!avtale) {
    return avtaleFinnesIkke();
  }

  if (avtale?.sluttDato && new Date(avtale.sluttDato) < new Date()) {
    return avtalenErAvsluttet(redigeringsModus!);
  }

  if (!avtale?.navRegion) {
    return avtaleManglerNavRegionError(avtale?.id);
  }
}
