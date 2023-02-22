import { Alert } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import {
  Grid,
  Metadata,
  Separator,
} from "../../components/detaljside/Metadata";
import { Laster } from "../../components/Laster";
import { formaterDato } from "../../utils/Utils";
import styles from "./Avtaleinfo.module.scss";

export function Avtaleinfo() {
  const { data: avtale, isLoading, error } = useAvtale();

  if (!avtale && isLoading) {
    return <Laster tekst="Laster avtaleinformasjon..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  if (!avtale) {
    return <Alert variant="warning">Fant ingen avtale</Alert>;
  }

  return (
    <div className={styles.container}>
      <dl>
        <Grid>
          <Metadata header="Startdato" verdi={formaterDato(avtale.startDato)} />
          <Metadata header="Sluttdato" verdi={formaterDato(avtale.sluttDato)} />
        </Grid>
        <Separator />
        <Grid>
          <Metadata header="Tiltakstype" verdi={avtale.tiltakstype.navn} />
          <Metadata header="Enhet" verdi={avtale.navEnhet?.navn} />
          <Metadata header="Avtaletype" verdi={avtale.avtaletype} />
          <Metadata header="Avtalenr" verdi={avtale.avtalenummer} />
          <Metadata header="Leverandør" verdi={avtale.leverandor?.navn} />
        </Grid>
        <Separator />
        <Metadata
          header="Pris og betalingsbetingelser"
          verdi={avtale.prisbetingelser}
        />
      </dl>
    </div>
  );
}
