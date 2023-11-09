import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, Heading } from "@navikt/ds-react";
import { Avtalestatus, Avtaletype, Toggles } from "mulighetsrommet-api-client";
import { Fragment, useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useDeleteAvtale } from "../../api/avtaler/useDeleteAvtale";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Bolk } from "../../components/detaljside/Bolk";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { VisHvisVerdi } from "../../components/detaljside/VisHvisVerdi";
import { Laster } from "../../components/laster/Laster";
import SlettAvtaleGjennomforingModal from "../../components/modal/SlettAvtaleGjennomforingModal";
import { addYear, avtaletypeTilTekst, formaterDato } from "../../utils/Utils";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import styles from "../DetaljerInfo.module.scss";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";

export function AvtaleInfo() {
  const { data: avtale, isLoading, error } = useAvtale();
  const [slettModal, setSlettModal] = useState(false);
  const mutation = useDeleteAvtale();
  const { data: enableOpsjoner } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPSJONER_FOR_AVTALER,
  );

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
      tekst = `Se originalavtale i Mercell `;
    } else if (avtale?.url?.includes("websak")) {
      tekst = `Se originalavtale i WebSak `;
    } else {
      tekst = `Se originalavtale `;
    }
    return (
      <>
        {tekst}
        <ExternalLinkIcon />
      </>
    );
  };

  function visKnapperad(avtalestatus: Avtalestatus): boolean {
    return avtalestatus === Avtalestatus.AKTIV;
  }

  return (
    <div className={styles.info_container}>
      <div>
        {visKnapperad(avtale.avtalestatus) && (
          <AvtaleKnapperad avtale={avtale} handleSlett={() => setSlettModal(true)} />
        )}
        <Separator />
      </div>
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
            <Metadata header="Avtaletype" verdi={avtaletypeTilTekst(avtale.avtaletype)} />
          </Bolk>

          <Separator />

          <Heading size="small" as="h3">
            Avtalens varighet
          </Heading>

          <Bolk aria-label="Start- og sluttdato">
            <Metadata header="Startdato" verdi={formaterDato(avtale.startDato)} />
            <Metadata header="Sluttdato" verdi={formaterDato(avtale.sluttDato)} />
            {enableOpsjoner && avtale.avtaletype === Avtaletype.RAMMEAVTALE && avtale.sluttDato ? (
              <Metadata
                header="Maks varighet inkl. opsjon"
                verdi={formaterDato(addYear(new Date(avtale.sluttDato), 5))}
              />
            ) : null}
          </Bolk>

          <Separator />

          <Bolk aria-label="Pris- og betalingsbetingelser">
            {erAnskaffetTiltak(avtale.tiltakstype.arenaKode) && (
              <Metadata
                header="Pris- og betalingsbetingelser"
                verdi={
                  avtale.prisbetingelser ??
                  "Det eksisterer ikke pris og betalingsbetingelser for denne avtalen"
                }
              />
            )}
          </Bolk>

          <VisHvisVerdi verdi={avtale?.url}>
            <a href={avtale.url!} target="_blank" rel="noopener noreferrer">
              {lenketekst()}
            </a>
          </VisHvisVerdi>

          <VisHvisVerdi verdi={avtale.administratorer}>
            <Bolk aria-label="Administratorer for avtalen">
              <Metadata
                header="Administratorer for avtalen"
                verdi={
                  avtale?.administratorer?.length ? (
                    <ul>
                      {avtale.administratorer?.map((admin) => {
                        return (
                          <li key={admin.navIdent}>
                            <a
                              target="_blank"
                              rel="noopener noreferrer"
                              href={`${NOM_ANSATT_SIDE}${admin?.navIdent}`}
                            >
                              {`${admin?.navn} - ${admin?.navIdent}`} <ExternalLinkIcon />
                            </a>
                          </li>
                        );
                      })}
                    </ul>
                  ) : (
                    "Ingen administratorer satt for avtalen"
                  )
                }
              />
            </Bolk>
          </VisHvisVerdi>
        </div>

        <div className={styles.detaljer}>
          {avtale.kontorstruktur.length > 1 ? (
            <Metadata
              header="Fylkessamarbeid"
              verdi={
                <ul>
                  {avtale.kontorstruktur.map((kontor) => {
                    return <li key={kontor.region.enhetsnummer}>{kontor.region.navn}</li>;
                  })}
                </ul>
              }
            />
          ) : (
            avtale?.kontorstruktur.map((struktur, index) => {
              return (
                <Fragment key={index}>
                  <Bolk aria-label="NAV-region">
                    <Metadata header="NAV-region" verdi={struktur.region.navn} />
                  </Bolk>

                  <Bolk aria-label="NAV-enheter">
                    <Metadata
                      header="NAV-enheter (kontorer)"
                      verdi={
                        <ul>
                          {struktur.kontorer.map((kontor) => (
                            <li key={kontor.enhetsnummer}>{kontor.navn}</li>
                          ))}
                        </ul>
                      }
                    />
                  </Bolk>
                </Fragment>
              );
            })
          )}

          <Separator />

          <Bolk aria-label="Tiltaksleverandør hovedenhet">
            <Metadata
              header="Tiltaksleverandør hovedenhet"
              verdi={[avtale.leverandor.navn, avtale.leverandor.organisasjonsnummer]
                .filter(Boolean)
                .join(" - ")}
            />
          </Bolk>

          <Bolk aria-label="Arrangører underenheter">
            <Metadata
              header="Arrangører underenheter"
              verdi={
                <ul>
                  {avtale.leverandorUnderenheter
                    .filter((enhet) => enhet.navn)
                    .map((enhet) => (
                      <li key={enhet.organisasjonsnummer}>
                        {enhet.navn} - {enhet.organisasjonsnummer}
                      </li>
                    ))}
                </ul>
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
      </div>
      <SlettAvtaleGjennomforingModal
        modalOpen={slettModal}
        handleCancel={() => setSlettModal(false)}
        data={avtale}
        mutation={mutation}
        dataType="avtale"
      />
    </div>
  );
}
