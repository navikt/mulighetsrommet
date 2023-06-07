import { useState } from "react";
import { Alert, Button } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Laster } from "../../components/laster/Laster";
import {
  capitalizeEveryWord,
  formaterDato,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { NavLink, useParams } from "react-router-dom";
import OpprettAvtaleModal from "../../components/avtaler/OpprettAvtaleModal";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import SlettAvtaleModal from "../../components/avtaler/SlettAvtaleModal";

export function Avtaleinfo() {
  const { avtaleId } = useParams<{ avtaleId: string }>();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading, error } = useAvtale(avtaleId);
  const { data: features } = useFeatureToggles();
  const [redigerModal, setRedigerModal] = useState(false);
  const [slettModal, setSlettModal] = useState(false);

  const handleRediger = () => setRedigerModal(true);
  const lukkRedigerModal = () => setRedigerModal(false);
  const handleSlett = () => setSlettModal(true);
  const lukkSlettModal = () => setSlettModal(false);

  if (!avtale && isLoading) {
    return <Laster tekst="Laster avtaleinformasjon..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  if (!avtale) {
    return <Alert variant="warning">Fant ingen avtale</Alert>;
  }

  const lenketekst = () => {
    if (avtale.url!.includes("mercell")) {
      return (
        <>
          Se originalavtale i Mercell <ExternalLinkIcon />
        </>
      );
    } else if (avtale.url!.includes("websak")) {
      return (
        <>
          Se originalavtale i WebSak <ExternalLinkIcon />
        </>
      );
    } else
      return (
        <>
          Se originalavtale <ExternalLinkIcon />
        </>
      );
  };

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
          <Metadata header="Enhet" verdi={avtale.navRegion?.navn} />

          <Metadata header="Avtaletype" verdi={avtale.avtaletype} />
          <Metadata header="Avtalenr" verdi={avtale.avtalenummer} />
        </div>
        <div className={styles.bolk}>
          <Metadata
            header="Leverandør"
            verdi={
              capitalizeEveryWord(avtale.leverandor?.navn) ||
              avtale.leverandor?.organisasjonsnummer
            }
          />
        </div>
        <Separator />

        {tiltakstypekodeErAnskaffetTiltak(avtale.tiltakstype.arenaKode) ? (
          <>
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
          </>
        ) : null}

        {avtale.ansvarlig && (
          <div>
            <Metadata header="Avtaleansvarlig" verdi={avtale.ansvarlig} />
          </div>
        )}

        {avtale.url && (
          <NavLink
            key={avtale.url}
            to={avtale.url}
            className={({ isActive }) =>
              isActive ? styles.navlink_active : styles.navlink
            }
          >
            {lenketekst}
          </NavLink>
        )}
      </div>
      <div className={styles.knapperad}>
        <div>
          {features?.["mulighetsrommet.admin-flate-slett-avtale"] ? (
            <Button
              variant="tertiary-neutral"
              onClick={handleSlett}
              data-testid="slett-avtale"
              className={styles.slett_knapp}
            >
              Slett
            </Button>
          ) : null}
        </div>
        <div>
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
      </div>
      <OpprettAvtaleModal
        modalOpen={redigerModal}
        onClose={lukkRedigerModal}
        shouldCloseOnOverlayClick={true}
        avtale={avtale}
      />
      <SlettAvtaleModal
        modalOpen={slettModal}
        onClose={lukkSlettModal}
        shouldCloseOnOverlayClick={true}
        avtale={avtale}
        handleRediger={() => setRedigerModal(true)}
      />
    </div>
  );
}
