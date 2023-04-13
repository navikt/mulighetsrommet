import { useState } from "react";
import { Alert, Button } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Laster } from "../../components/laster/Laster";
import { capitalizeEveryWord, formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { NavLink } from "react-router-dom";
import OpprettAvtaleModal from "../../components/avtaler/opprett/OpprettAvtaleModal";

export function Avtaleinfo() {
  const { data: avtale, isLoading, error } = useAvtale();
  const { data: features } = useFeatureToggles();
  const [redigerModal, setRedigerModal] = useState(false);

  const handleRediger = () => setRedigerModal(true);
  const lukkRedigerModal = () => setRedigerModal(false);

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
          <Metadata header="Antall plasser" verdi={avtale.antallPlasser} />
          <Metadata
            header="Leverandør"
            verdi={
              capitalizeEveryWord(avtale.leverandor?.navn) ||
              avtale.leverandor?.organisasjonsnummer
            }
          />
          <Metadata header="Avtaletype" verdi={avtale.avtaletype} />
          <Metadata header="Avtalenr" verdi={avtale.avtalenummer} />
        </div>
        <Separator />
        <div>
          <Metadata
            header="Pris og betalingsbetingelser"
            verdi={
              avtale.prisbetingelser ??
              "Det eksisterer ikke pris og betalingsbetingelser for denne avtalen"
            }
          />
        </div>
        <Separator />
        <div>
          <Metadata header="Avtaleansvarlig" verdi={avtale.ansvarlig} />
        </div>
        {avtale.url && (
          <NavLink
            key={avtale.url}
            to={avtale.url}
            className={({ isActive }) =>
              isActive ? styles.navlink_active : styles.navlink
            }
          >
            Gå til avtalen i websak
          </NavLink>
        )}
      </div>
      <div className={styles.knapperad}>
        {features?.["mulighetsrommet.admin-flate-rediger-avtale"] ? (
          <Button
            variant="tertiary"
            onClick={handleRediger}
            data-testid="endre-avtale"
          >
            Endre
          </Button>
        ) : null}
      </div>
      <OpprettAvtaleModal
        modalOpen={redigerModal}
        onClose={lukkRedigerModal}
        shouldCloseOnOverlayClick={true}
        avtale={avtale}
      />
    </div>
  );
}
