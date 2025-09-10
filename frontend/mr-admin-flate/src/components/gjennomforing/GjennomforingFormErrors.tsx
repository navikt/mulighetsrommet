import { Link } from "react-router";
import { BodyShort } from "@navikt/ds-react";
import { avtaleHarRegioner } from "@/utils/Utils";
import { PORTEN_URL } from "@/constants";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";

const avtaleFinnesIkke = () => (
  <>
    <BodyShort>Det finnes ingen avtale koblet til tiltaksgjennomføringen.</BodyShort>
    <BodyShort>
      Ta <a href={PORTEN_URL}>kontakt i Porten</a> dersom du trenger mer hjelp.
    </BodyShort>
  </>
);

const avtaleManglerNavRegionError = (avtaleId?: string) => (
  <>
    <BodyShort>
      Avtalen mangler Nav region. Du må oppdatere avtalens Nav region for å kunne opprette en
      gjennomføring.
      {avtaleId ? (
        <Link reloadDocument to={`/avtaler/${avtaleId}`}>
          Klikk her for å fikse avtalen
        </Link>
      ) : null}
    </BodyShort>
    <BodyShort>
      Ta <a href={PORTEN_URL}>kontakt i Porten</a> dersom du trenger mer hjelp.
    </BodyShort>
  </>
);

export function ErrorMeldinger(avtale: AvtaleDto | undefined) {
  if (!avtale) {
    return avtaleFinnesIkke();
  }

  if (!avtaleHarRegioner(avtale)) {
    return avtaleManglerNavRegionError(avtale.id);
  }
}
