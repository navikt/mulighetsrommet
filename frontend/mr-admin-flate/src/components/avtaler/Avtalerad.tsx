import { BodyShort } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { capitalizeEveryWord, formaterDato } from "../../utils/Utils";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeRad } from "../listeelementer/ListeRad";
import { Avtalestatus } from "../statuselementer/Avtalestatus";

interface Props {
  avtale: Avtale;
}

export function Avtalerad({ avtale }: Props) {
  return (
    <ListeRad
      linkTo={`/avtaler/${avtale.id}`}
      classname={styles.listerad_avtale}
      testId="avtalerad"
    >
      <BodyShort aria-label={`Avtalenavn: ${avtale.navn}`}>
        {avtale.navn}
      </BodyShort>
      <BodyShort aria-label={`Leverandør: ${avtale.leverandor?.navn}`}>
        {capitalizeEveryWord(avtale.leverandor?.navn, ["og", "i"]) || ""}
      </BodyShort>
      <BodyShort
        aria-label={`NAV-enhet: ${
          avtale.navEnhet?.navn || avtale.navEnhet?.enhetsnummer
        }`}
      >
        {avtale.navEnhet?.navn || avtale?.navEnhet?.enhetsnummer}
      </BodyShort>

      <BodyShort
        title={`Startdato ${formaterDato(avtale.startDato)}`}
        aria-label={`Startdato: ${formaterDato(avtale.startDato)}`}
      >
        {formaterDato(avtale.startDato)}
      </BodyShort>
      <BodyShort
        title={`Sluttdato ${formaterDato(avtale.sluttDato)}`}
        aria-label={`Sluttdato: ${formaterDato(avtale.sluttDato)}`}
      >
        {formaterDato(avtale.sluttDato)}
      </BodyShort>
      <BodyShort>
        <Avtalestatus avtale={avtale} />
      </BodyShort>
    </ListeRad>
  );
}
