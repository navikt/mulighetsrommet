import { Alert, Button } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Laster } from "../../components/laster/Laster";
import { capitalizeEveryWord, formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";

export function Avtaleinfo() {
  const { data: avtale, isLoading, error } = useAvtale();
  const { data: features } = useFeatureToggles();

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
      <div className={styles.detaljer}>
        <div className={styles.bolk}>
          <Metadata header="Startdato" verdi={formaterDato(avtale.startDato)} />
          <Metadata header="Sluttdato" verdi={formaterDato(avtale.sluttDato)} />
        </div>
        <Separator />
        <div className={styles.bolk}>
          <Metadata header="Tiltakstype" verdi={avtale.tiltakstype.navn} />
          <Metadata header="Enhet" verdi={avtale.navEnhet?.navn} />
          <Metadata header="Avtaletype" verdi={avtale.avtaletype} />
          <Metadata header="Avtalenr" verdi={avtale.avtalenummer} />
          <Metadata
            header="Leverandør"
            verdi={
              capitalizeEveryWord(avtale.leverandor?.navn) ||
              avtale.leverandor?.organisasjonsnummer
            }
          />
        </div>
        <Separator />
        <div>
          <Metadata
            header="Pris og betalingsbetingelser"
            verdi={avtale.prisbetingelser}
          />
        </div>
      </div>
      <div className={styles.knapperad}>
        {features?.["mulighetsrommet.admin-flate-rediger-avtale"] ? (
          <Button
            variant="tertiary"
            onClick={() => alert("Endring av avtale er ikke klar enda")}
          >
            Endre
          </Button>
        ) : null}
      </div>
      {/**
       * TODO Vis modal med preutfylte felter basert på avtale man skal redigere.
       */}
      {/* <OpprettAvtaleModal
        modalOpen={false}
        onClose={() => alert("onClose not implemented")}
        shouldCloseOnOverlayClick={true}
      /> */}
    </div>
  );
}
