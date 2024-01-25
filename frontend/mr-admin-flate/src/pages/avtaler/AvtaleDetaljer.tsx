import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, Heading, HelpText } from "@navikt/ds-react";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";
import { Fragment } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Bolk } from "../../components/detaljside/Bolk";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Laster } from "../../components/laster/Laster";
import { avtaletypeTilTekst, formaterDato } from "../../utils/Utils";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import styles from "../DetaljerInfo.module.scss";
import { Link } from "react-router-dom";
import { NavEnhet } from "mulighetsrommet-api-client";

export function AvtaleDetaljer() {
  const { data: avtale, isPending, error } = useAvtale();

  if (isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  const lenketekst = () => {
    let tekst;
    if (avtale?.url?.includes("websak")) {
      tekst = `Se originalavtale i WebSak `;
    } else {
      tekst = `Se originalavtale `;
    }
    return (
      <>
        {tekst}
        <ExternalLinkIcon aria-label="Ekstern lenke" />
      </>
    );
  };

  function sorterPaRegionsnavn(a: { region: NavEnhet }, b: { region: NavEnhet }) {
    return a.region.navn.localeCompare(b.region.navn);
  }

  const {
    navn,
    avtalenummer,
    tiltakstype,
    avtaletype,
    startDato,
    sluttDato,
    administratorer,
    url,
    kontorstruktur,
    arenaAnsvarligEnhet,
    leverandor,
    leverandorUnderenheter,
    leverandorKontaktperson,
  } = avtale;

  return (
    <div className={styles.container}>
      <div className={styles.detaljer}>
        <Bolk aria-label="Avtalenavn og avtalenummer">
          <Metadata header="Avtalenavn" verdi={navn} />
          <Metadata header="Avtalenummer" verdi={avtalenummer} />
        </Bolk>

        <Bolk aria-label="Tiltakstype">
          <Metadata
            header="Tiltakstype"
            verdi={<Link to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Link>}
          />
        </Bolk>

        <Bolk aria-label="Avtaletype">
          <Metadata header="Avtaletype" verdi={avtaletypeTilTekst(avtaletype)} />
        </Bolk>

        <Separator />

        <Heading size="small" as="h3">
          Avtalens varighet
        </Heading>

        <Bolk aria-label="Start- og sluttdato">
          <Metadata header="Startdato" verdi={formaterDato(startDato)} />
          <Metadata header="Sluttdato" verdi={formaterDato(sluttDato)} />
        </Bolk>

        <Separator />

        <Bolk aria-label="Pris- og betalingsbetingelser">
          {erAnskaffetTiltak(tiltakstype.arenaKode) && (
            <Metadata
              header="Pris- og betalingsbetingelser"
              verdi={
                avtale.prisbetingelser ??
                "Det eksisterer ikke pris og betalingsbetingelser for denne avtalen"
              }
            />
          )}
        </Bolk>

        {url ? (
          <a href={url} target="_blank" rel="noopener noreferrer">
            {lenketekst()}
          </a>
        ) : null}

        {administratorer ? (
          <Bolk aria-label="Administratorer for avtalen">
            <Metadata
              header="Administratorer for avtalen"
              verdi={
                administratorer.length ? (
                  <ul>
                    {administratorer.map((admin) => {
                      return (
                        <li key={admin.navIdent}>
                          <a
                            target="_blank"
                            rel="noopener noreferrer"
                            href={`${NOM_ANSATT_SIDE}${admin.navIdent}`}
                          >
                            {`${admin.navn} - ${admin.navIdent}`}{" "}
                            <ExternalLinkIcon aria-label="Ekstern lenke" />
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
        ) : null}
      </div>

      <div className={styles.detaljer}>
        {kontorstruktur.length > 1 ? (
          <Metadata
            header="Fylkessamarbeid"
            verdi={
              <ul>
                {kontorstruktur.sort(sorterPaRegionsnavn).map((kontor) => {
                  return <li key={kontor.region.enhetsnummer}>{kontor.region.navn}</li>;
                })}
              </ul>
            }
          />
        ) : (
          kontorstruktur.map((struktur, index) => {
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
        {arenaAnsvarligEnhet ? (
          <div style={{ display: "flex", gap: "1rem", margin: "0.5rem 0" }}>
            <dl style={{ margin: "0" }}>
              <Metadata
                header="Ansvarlig enhet fra Arena"
                verdi={`${arenaAnsvarligEnhet.enhetsnummer} ${arenaAnsvarligEnhet.navn}`}
              />
            </dl>
            <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
              Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet når
              det opprettes avtale i Arena.
            </HelpText>
          </div>
        ) : null}

        <Separator />

        <Bolk aria-label="Tiltaksleverandør hovedenhet">
          <Metadata
            header="Tiltaksleverandør hovedenhet"
            verdi={[leverandor.navn, leverandor.organisasjonsnummer].filter(Boolean).join(" - ")}
          />
        </Bolk>

        <Bolk aria-label="Arrangører underenheter">
          <Metadata
            header="Arrangører underenheter"
            verdi={
              <ul>
                {leverandorUnderenheter.map((enhet) => (
                  <li key={enhet.organisasjonsnummer}>
                    {enhet?.navn
                      ? `${enhet.navn} - ${enhet.organisasjonsnummer}`
                      : `${enhet.organisasjonsnummer}`}
                  </li>
                ))}
              </ul>
            }
          />
        </Bolk>

        <Separator />

        {leverandorKontaktperson ? (
          <Bolk aria-label="Kontaktperson">
            <Metadata
              header="Kontaktperson"
              verdi={
                <div className={styles.leverandor_kontaktinfo}>
                  <label>{leverandorKontaktperson.navn}</label>
                  <label>{leverandorKontaktperson.telefon}</label>
                  <a href={`mailto:${leverandorKontaktperson.epost}`}>
                    {leverandorKontaktperson.epost}
                  </a>
                  <label>{leverandorKontaktperson.beskrivelse}</label>
                </div>
              }
            />
          </Bolk>
        ) : null}
      </div>
    </div>
  );
}
