import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, Button, Heading } from "@navikt/ds-react";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import OpprettAvtaleModal from "../../components/avtaler/OpprettAvtaleModal";
import SlettAvtaleModal from "../../components/avtaler/SlettAvtaleModal";
import { Bolk } from "../../components/detaljside/Bolk";
import {
  Liste,
  Metadata,
  Separator,
} from "../../components/detaljside/Metadata";
import { VisHvisVerdi } from "../../components/detaljside/VisHvisVerdi";
import { Laster } from "../../components/laster/Laster";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import {
  avtaletypeTilTekst,
  formaterDato,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import {
  AvtaleAvslutningsstatus,
  Avtalestatus,
} from "mulighetsrommet-api-client";

export function Avtaleinfo() {
  const avtaleId = useGetAvtaleIdFromUrl();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading, error, refetch } = useAvtale();
  const { data: features } = useFeatureToggles();
  const [redigerModal, setRedigerModal] = useState(false);
  const [slettModal, setSlettModal] = useState(false);

  const handleRediger = () => setRedigerModal(true);
  const lukkRedigerModal = () => {
    refetch();
    setRedigerModal(false);
  };
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
    if (avtale?.url?.includes("mercell")) {
      return (
        <>
          Se originalavtale i Mercell <ExternalLinkIcon />
        </>
      );
    } else if (avtale?.url?.includes("websak")) {
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
        <Bolk aria-label="Avtalenavn">
          <Metadata header="Avtalenavn" verdi={avtale.navn} />
          <VisHvisVerdi verdi={avtale.avtalenummer}>
            <Metadata header="Avtalenr" verdi={avtale.avtalenummer} />
          </VisHvisVerdi>
        </Bolk>
        <Bolk aria-label="Tiltakstype">
          <Metadata header="Tiltakstype" verdi={avtale.tiltakstype.navn} />
        </Bolk>
        <Bolk aria-label="Avtaletype">
          <Metadata
            header="Avtaletype"
            verdi={avtaletypeTilTekst(avtale.avtaletype)}
          />
        </Bolk>
        <Separator />
        <Heading size="small" as="h3">
          Avtalens varighet
        </Heading>
        <Bolk aria-label="Start- og sluttdato">
          <Metadata header="Startdato" verdi={formaterDato(avtale.startDato)} />
          <Metadata header="Sluttdato" verdi={formaterDato(avtale.sluttDato)} />
        </Bolk>
        <Separator />

        <Bolk aria-label="Pris- og betalingsbetingelser">
          {tiltakstypekodeErAnskaffetTiltak(avtale.tiltakstype.arenaKode) ? (
            <Metadata
              header="Pris- og betalingsbetingelser"
              verdi={
                avtale.prisbetingelser ??
                "Det eksisterer ikke pris og betalingsbetingelser for denne avtalen"
              }
            />
          ) : null}
        </Bolk>
        <VisHvisVerdi verdi={avtale?.url}>
          <a href={avtale.url!} target="_blank" rel="noopener noreferrer">
            {lenketekst()}
          </a>
        </VisHvisVerdi>
        <VisHvisVerdi verdi={avtale.ansvarlig?.navident}>
          <Bolk aria-label="Avtaleansvarlig">
            <Metadata
              header="Avtaleansvarlig"
              verdi={`${avtale.ansvarlig?.navn} - ${avtale.ansvarlig?.navident}`}
            />
          </Bolk>
        </VisHvisVerdi>
      </div>

      <div className={styles.detaljer}>
        <Bolk aria-label="NAV-region">
          <Metadata header="NAV-region" verdi={avtale.navRegion?.navn} />
        </Bolk>
        <Bolk aria-label="NAV-enheter">
          <Metadata
            header="NAV-enheter (kontorer)"
            verdi={
              <Liste
                elementer={avtale.navEnheter.map((enhet) => ({
                  key: enhet.enhetsnummer,
                  value: enhet.navn,
                }))}
                tekstHvisTom="Alle enheter"
              />
            }
          />{" "}
        </Bolk>
        <Separator />

        <Bolk aria-label="Tiltaksleverandør hovedenhet">
          <Metadata
            header="Tiltaksleverandør hovedenhet"
            verdi={[
              avtale.leverandor.navn,
              avtale.leverandor.organisasjonsnummer,
            ]
              .filter(Boolean)
              .join(" - ")}
          />
        </Bolk>

        <Bolk aria-label="Arrangører underenheter">
          <Metadata
            header="Arrangører underenheter"
            verdi={
              <Liste
                elementer={avtale.leverandorUnderenheter
                  .filter((enhet) => enhet.navn)
                  .map((enhet) => ({
                    key: enhet.organisasjonsnummer,
                    value: `${enhet.navn} - ${enhet.organisasjonsnummer}`,
                  }))}
                tekstHvisTom="Alle underenheter for arrangør"
              />
            }
          />
        </Bolk>

        <Separator />

        <VisHvisVerdi verdi={avtale.leverandorKontaktperson}>
          <Bolk aria-label="Kontaktperson">
            <Metadata
              header="Kontaktperson"
              verdi={
                <div className={styles.leverandor_kontaktinfo}>
                  <label>{avtale.leverandorKontaktperson?.navn}</label>
                  <label>{avtale.leverandorKontaktperson?.telefon}</label>
                  <a href={`mailto:${avtale.leverandorKontaktperson?.epost}`}>
                    {avtale.leverandorKontaktperson?.epost}
                  </a>
                  {<label>{avtale.leverandorKontaktperson?.beskrivelse}</label>}
                </div>
              }
            />
          </Bolk>
        </VisHvisVerdi>
      </div>

      {visKnapperad(avtale.avtalestatus) ? (
        <div className={styles.knapperad}>
          <div>
            {features?.["mulighetsrommet.admin-flate-slett-avtale"] ? (
              <Button
                variant="tertiary-neutral"
                onClick={handleSlett}
                data-testid="slett-avtale"
                className={styles.slett_knapp}
              >
                Feilregistrering
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
      ) : null}
      <OpprettAvtaleModal
        modalOpen={redigerModal}
        onClose={lukkRedigerModal}
        onSuccess={() => {
          lukkRedigerModal();
          refetch();
        }}
        avtale={avtale}
      />
      <SlettAvtaleModal
        modalOpen={slettModal}
        onClose={lukkSlettModal}
        avtale={avtale}
        handleRediger={() => setRedigerModal(true)}
      />
    </div>
  );
}

function visKnapperad(avtalestatus: Avtalestatus): boolean {
  const whitelist: Avtalestatus[] = [Avtalestatus.AKTIV, Avtalestatus.PLANLAGT];

  return whitelist.includes(avtalestatus);
}
