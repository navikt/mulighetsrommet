import { Alert, Heading } from "@navikt/ds-react";
import { Avtalestatus } from "mulighetsrommet-api-client";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import AvbrytAvtaleModal from "../../components/avtaler/AvbrytAvtaleModal";
import { Bolk } from "../../components/detaljside/Bolk";
import {
  Liste,
  Metadata,
  Separator,
} from "../../components/detaljside/Metadata";
import { VisHvisVerdi } from "../../components/detaljside/VisHvisVerdi";
import { Laster } from "../../components/laster/Laster";
import {
  avtaletypeTilTekst,
  formaterDato,
  tiltakstypekodeErAnskaffetTiltak,
} from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import SlettAvtaleGjennomforingModal from "../../components/modal/SlettAvtaleGjennomforingModal";
import { useDeleteAvtale } from "../../api/avtaler/useDeleteAvtale";

export function Avtaleinfo() {
  const { data: avtale, isLoading, error, refetch } = useAvtale();
  const [slettModal, setSlettModal] = useState(false);
  const [avbrytModal, setAvbrytModal] = useState(false);
  const mutation = useDeleteAvtale();

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
    let tekst;
    if (avtale?.url?.includes("mercell")) {
      tekst = `Se originalavtale i Mercell <ExternalLinkIcon />`;
    } else if (avtale?.url?.includes("websak")) {
      tekst = `Se originalavtale i WebSak <ExternalLinkIcon />`;
    } else {
      tekst = `Se originalavtale <ExternalLinkIcon />`;
    }
    return <>{tekst}</>;
  };

  function visKnapperad(avtalestatus: Avtalestatus): boolean {
    const whitelist: Avtalestatus[] = [
      Avtalestatus.AKTIV,
      Avtalestatus.PLANLAGT,
    ];

    return whitelist.includes(avtalestatus);
  }

  return (
    <>
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
            <Metadata
              header="Startdato"
              verdi={formaterDato(avtale.startDato)}
            />
            <Metadata
              header="Sluttdato"
              verdi={formaterDato(avtale.sluttDato)}
            />
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
                    {
                      <label>
                        {avtale.leverandorKontaktperson?.beskrivelse}
                      </label>
                    }
                  </div>
                }
              />
            </Bolk>
          </VisHvisVerdi>
        </div>

        {visKnapperad(avtale.avtalestatus) ? (
          <AvtaleKnapperad
            avtale={avtale}
            handleSlett={() => setSlettModal(true)}
            handleAvbryt={() => setAvbrytModal(true)}
          />
        ) : null}
      </div>
      <SlettAvtaleGjennomforingModal
        modalOpen={slettModal}
        handleCancel={() => setSlettModal(false)}
        data={avtale}
        mutation={mutation}
        dataType="avtale"
      />
      <AvbrytAvtaleModal
        modalOpen={avbrytModal}
        onClose={() => {
          refetch();
          setAvbrytModal(false);
        }}
        avtale={avtale}
      />
    </>
  );
}
