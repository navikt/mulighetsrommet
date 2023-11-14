import { ExclamationmarkTriangleIcon, ExternalLinkIcon } from "@navikt/aksel-icons";
import { Heading, HelpText } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
} from "mulighetsrommet-api-client";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";
import { Bolk } from "../../components/detaljside/Bolk";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { VisHvisVerdi } from "../../components/detaljside/VisHvisVerdi";
import { erProdMiljo, formaterDato, tilgjengelighetsstatusTilTekst } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { Kontaktperson } from "./Kontaktperson";
import { Link } from "react-router-dom";
import { useTitle } from "mulighetsrommet-frontend-common";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  avtale?: Avtale;
}

export function TiltaksgjennomforingDetaljer(props: Props) {
  const { tiltaksgjennomforing, avtale } = props;
  useTitle(`Tiltaksgjennomføring - ${tiltaksgjennomforing.navn}`);

  const navnPaaNavEnheterForKontaktperson = (enheterForKontaktperson: string[]): string => {
    return (
      tiltaksgjennomforing?.navEnheter
        .map((enhet) => {
          return enheterForKontaktperson
            .map((kp) => {
              if (enhet.enhetsnummer === kp) {
                return enhet.navn;
              }
              return null;
            })
            .join("");
        })
        .filter(Boolean)
        .join(", ") || "alle enheter"
    );
  };

  const sanityTiltaksgjennomforingUrl =
    "https://mulighetsrommet-sanity-studio.intern.nav.no/" +
    (erProdMiljo ? "prod" : "test") +
    "/desk/tiltaksgjennomforinger;alleTiltaksgjennomforinger;";

  const todayDate = new Date();
  const kontaktpersonerFraNav = tiltaksgjennomforing.kontaktpersoner ?? [];

  return (
    <>
      <div className={styles.container}>
        <div className={styles.detaljer}>
          <Bolk aria-label="Tiltakstype">
            <Metadata header="Tiltakstype" verdi={tiltaksgjennomforing.tiltakstype.navn} />

            <VisHvisVerdi verdi={tiltaksgjennomforing.tiltaksnummer}>
              <Bolk aria-label="Tiltaksnummer">
                <Metadata header="Tiltaksnummer" verdi={tiltaksgjennomforing.tiltaksnummer} />
              </Bolk>
            </VisHvisVerdi>
          </Bolk>

          <Bolk aria-label="Avtale">
            <Metadata
              header="Avtale"
              verdi={
                avtale?.id ? (
                  <>
                    <Link to={`/avtaler/${avtale?.id}`}>
                      {avtale?.navn} {avtale?.avtalenummer ? ` - ${avtale.avtalenummer}` : null}
                    </Link>{" "}
                  </>
                ) : (
                  "Ingen avtale for gjennomføringen"
                )
              }
            />
          </Bolk>

          <Separator />

          <Bolk aria-label="Start- og sluttdato">
            <Metadata header="Startdato" verdi={formaterDato(tiltaksgjennomforing.startDato)} />
            <Metadata header="Sluttdato" verdi={formaterDato(tiltaksgjennomforing.sluttDato)} />
          </Bolk>

          <Bolk aria-label="Oppstartsdato">
            <Metadata
              header="Oppstart"
              verdi={
                tiltaksgjennomforing.oppstart === TiltaksgjennomforingOppstartstype.FELLES
                  ? "Felles"
                  : "Løpende oppstart"
              }
            />
            {Boolean(tiltaksgjennomforing.stengtFra) &&
              Boolean(tiltaksgjennomforing.stengtTil) &&
              new Date(tiltaksgjennomforing.stengtTil!!) > todayDate && (
                <Metadata
                  header={
                    todayDate >= new Date(tiltaksgjennomforing.stengtFra!!) &&
                    todayDate <= new Date(tiltaksgjennomforing.stengtTil!!) ? (
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "row",
                        }}
                      >
                        <ExclamationmarkTriangleIcon
                          style={{
                            marginRight: "5px",
                          }}
                          title="midlertidig-stengt"
                        />
                        <Heading size="xsmall" level="3">
                          Midlertidig Stengt
                        </Heading>
                      </div>
                    ) : (
                      <Heading size="xsmall" level="3">
                        Midlertidig Stengt
                      </Heading>
                    )
                  }
                  verdi={
                    formaterDato(tiltaksgjennomforing.stengtFra) +
                    " - " +
                    formaterDato(tiltaksgjennomforing.stengtTil)
                  }
                />
              )}
          </Bolk>

          <Bolk aria-label="Antall plasser">
            <Metadata header="Antall plasser" verdi={tiltaksgjennomforing.antallPlasser} />
          </Bolk>

          <Separator />

          <Bolk aria-label="Tilgjengelighetsstatus">
            <Metadata
              header="Tilgjengelighetsstatus"
              verdi={tilgjengelighetsstatusTilTekst(tiltaksgjennomforing.tilgjengelighet)}
            />
          </Bolk>

          <Separator />

          <Bolk aria-label="Administratorer for gjennomføringen">
            <Metadata
              header="Administratorer for gjennomføringen"
              verdi={
                tiltaksgjennomforing?.administratorer?.length ? (
                  <ul>
                    {tiltaksgjennomforing.administratorer?.map((admin) => {
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
                  "Ingen administratorer satt for gjennomføringen"
                )
              }
            />
          </Bolk>

          <VisHvisVerdi verdi={tiltaksgjennomforing.sanityId}>
            <Separator />
            <Bolk aria-label="Sanity-dokument">
              <Metadata
                header="Sanity dokument"
                verdi={
                  <>
                    <Link
                      target="_blank"
                      to={sanityTiltaksgjennomforingUrl + tiltaksgjennomforing.sanityId}
                    >
                      Åpne tiltaksgjennomføringen i Sanity{" "}
                      <ExternalLinkIcon title="Åpner tiltaksgjennomføringen i Sanity" />
                    </Link>
                  </>
                }
              />
            </Bolk>
          </VisHvisVerdi>
        </div>

        <div className={styles.detaljer}>
          <Bolk aria-label="NAV-region">
            <Metadata header="NAV-region" verdi={tiltaksgjennomforing?.navRegion?.navn} />
          </Bolk>

          <Bolk aria-label="NAV-enheter">
            <Metadata
              header="NAV-enheter (kontorer)"
              verdi={
                <ul>
                  {tiltaksgjennomforing.navEnheter.map((enhet) => (
                    <li key={enhet.enhetsnummer}>{enhet.navn}</li>
                  ))}
                </ul>
              }
            />
          </Bolk>

          {tiltaksgjennomforing?.arenaAnsvarligEnhet ? (
            <Bolk>
              <div style={{ display: "flex", gap: "1rem" }}>
                <Metadata
                  header="Ansvarlig enhet fra Arena"
                  verdi={tiltaksgjennomforing.arenaAnsvarligEnhet}
                />
                <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
                  Ansvarlig enhet fra Arena blir satt i Arena når man oppretter tiltak i Arena.
                </HelpText>
              </div>
            </Bolk>
          ) : null}

          {kontaktpersonerFraNav.map((kp, index) => {
            return (
              <Bolk
                aria-label={`Kontaktperson hos ${navnPaaNavEnheterForKontaktperson(kp.navEnheter)}`}
                key={index}
              >
                <Metadata
                  header={`Kontaktperson hos ${navnPaaNavEnheterForKontaktperson(kp.navEnheter)}`}
                  verdi={<Kontaktperson kontaktperson={kp} />}
                />
              </Bolk>
            );
          })}

          <Separator />

          <VisHvisVerdi verdi={avtale?.leverandor}>
            <Bolk aria-label="Tiltaksleverandør hovedenhet">
              <Metadata
                header="Tiltaksleverandør hovedenhet"
                verdi={[avtale?.leverandor.navn, avtale?.leverandor.organisasjonsnummer]
                  .filter(Boolean)
                  .join(" - ")}
              />
            </Bolk>
          </VisHvisVerdi>

          <VisHvisVerdi verdi={tiltaksgjennomforing.arrangor.navn}>
            <Bolk aria-label="Arrangør underenhet">
              <Metadata
                header="Arrangør underenhet"
                verdi={`${tiltaksgjennomforing.arrangor.navn} - ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
              />
            </Bolk>
          </VisHvisVerdi>

          <VisHvisVerdi verdi={tiltaksgjennomforing.stedForGjennomforing}>
            <Bolk aria-label="Sted for gjennomføringen">
              <Metadata
                header="Sted for gjennomføringen"
                verdi={tiltaksgjennomforing.stedForGjennomforing}
              />
            </Bolk>
          </VisHvisVerdi>

          {tiltaksgjennomforing.arrangor.kontaktperson && (
            <Metadata
              header="Kontaktperson hos arrangør"
              verdi={
                <div className={styles.leverandor_kontaktinfo}>
                  <label>{tiltaksgjennomforing.arrangor.kontaktperson?.navn}</label>
                  <label>{tiltaksgjennomforing.arrangor.kontaktperson?.telefon}</label>
                  <a href={`mailto:${tiltaksgjennomforing.arrangor.kontaktperson?.epost}`}>
                    {tiltaksgjennomforing.arrangor.kontaktperson?.epost}
                  </a>
                  {tiltaksgjennomforing.arrangor.kontaktperson?.beskrivelse && (
                    <label>{tiltaksgjennomforing.arrangor.kontaktperson?.beskrivelse}</label>
                  )}
                </div>
              }
            />
          )}
        </div>
      </div>
    </>
  );
}
