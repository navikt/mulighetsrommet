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
import { formaterDato } from "../../utils/Utils";
import styles from "../DetaljerInfo.module.scss";
import { Kontaktperson } from "./Kontaktperson";
import { Link } from "react-router-dom";
import { useTitle } from "mulighetsrommet-frontend-common";
import {
  fremmoteDatoFromTidspunkt,
  fremmoteTidFromTidspunkt,
} from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaConst";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
  avtale?: Avtale;
}

export function TiltaksgjennomforingDetaljer(props: Props) {
  const { tiltaksgjennomforing, avtale } = props;
  useTitle(
    `Tiltaksgjennomføring ${tiltaksgjennomforing.navn ? `- ${tiltaksgjennomforing.navn}` : null}`,
  );

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

  const todayDate = new Date();
  const kontaktpersonerFraNav = tiltaksgjennomforing.kontaktpersoner ?? [];

  const {
    tiltakstype,
    tiltaksnummer,
    startDato,
    sluttDato,
    oppstart,
    stengtFra,
    stengtTil,
    antallPlasser,
    deltidsprosent,
    apentForInnsok,
    administratorer,
    navRegion,
    navEnheter,
    arenaAnsvarligEnhet,
    arrangor,
    fremmoteTidspunkt,
    fremmoteSted,
    stedForGjennomforing,
  } = tiltaksgjennomforing;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.detaljer}>
          <Bolk aria-label="Tiltaksnavn og tiltaksnummer" data-testid="tiltaksnavn">
            <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.navn} />
            {tiltaksnummer ? <Metadata header="Tiltaksnummer" verdi={tiltaksnummer} /> : null}
          </Bolk>

          <Bolk aria-label="Tiltakstype og avtaletype">
            <Metadata
              header="Avtale"
              verdi={
                avtale?.id ? (
                  <>
                    <Link to={`/avtaler/${avtale.id}`}>
                      {avtale.navn} {avtale.avtalenummer ? ` - ${avtale.avtalenummer}` : null}
                    </Link>{" "}
                  </>
                ) : (
                  "Ingen avtale for gjennomføringen"
                )
              }
            />
            <Metadata header="Tiltakstype" verdi={tiltakstype.navn} />
          </Bolk>

          <Separator />

          <Bolk aria-label="Start- og sluttdato">
            <Metadata header="Startdato" verdi={formaterDato(startDato)} />
            <Metadata header="Sluttdato" verdi={formaterDato(sluttDato)} />
          </Bolk>

          <Bolk aria-label="Oppstartsdato">
            <Metadata
              header="Oppstart"
              verdi={
                oppstart === TiltaksgjennomforingOppstartstype.FELLES
                  ? "Felles"
                  : "Løpende oppstart"
              }
            />
            {stengtFra && stengtTil && new Date(stengtTil) > todayDate && (
              <Metadata
                header={
                  todayDate >= new Date(stengtFra) && todayDate <= new Date(stengtTil) ? (
                    <div style={{ display: "flex", flexDirection: "row" }}>
                      <ExclamationmarkTriangleIcon
                        style={{ marginRight: "5px" }}
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
                verdi={formaterDato(stengtFra) + " - " + formaterDato(stengtTil)}
              />
            )}
          </Bolk>

          <Bolk aria-label="Antall plasser">
            <Metadata header="Antall plasser" verdi={antallPlasser} />
            <Metadata header="Deltidsprosent" verdi={deltidsprosent} />
          </Bolk>

          <Separator />

          <Bolk aria-label="Åpent for innsøk">
            <Metadata header="Åpent for innsøk" verdi={apentForInnsok ? "Ja" : "Nei"} />
          </Bolk>

          <Separator />

          <Bolk aria-label="Administratorer for gjennomføringen">
            <Metadata
              header="Administratorer for gjennomføringen"
              verdi={
                administratorer?.length ? (
                  <ul>
                    {administratorer.map((admin) => {
                      return (
                        <li key={admin.navIdent}>
                          <a
                            target="_blank"
                            rel="noopener noreferrer"
                            href={`${NOM_ANSATT_SIDE}${admin?.navIdent}`}
                          >
                            {`${admin?.navn} - ${admin?.navIdent}`}{" "}
                            <ExternalLinkIcon aria-label="Ekstern lenke" />
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
        </div>

        <div className={styles.detaljer}>
          <Bolk aria-label="NAV-region">
            <Metadata header="NAV-region" verdi={navRegion?.navn} />
          </Bolk>

          <Bolk aria-label="NAV-enheter">
            <Metadata
              header="NAV-enheter (kontorer)"
              verdi={
                <ul>
                  {navEnheter.map((enhet) => (
                    <li key={enhet.enhetsnummer}>{enhet.navn}</li>
                  ))}
                </ul>
              }
            />
          </Bolk>

          {arenaAnsvarligEnhet ? (
            <Bolk>
              <div style={{ display: "flex", gap: "1rem" }}>
                <Metadata
                  header="Ansvarlig enhet fra Arena"
                  verdi={`${arenaAnsvarligEnhet.enhetsnummer} ${arenaAnsvarligEnhet.navn}`}
                />
                <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
                  Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet
                  når man oppretter tiltak i Arena.
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

          {avtale?.leverandor ? (
            <Bolk aria-label="Tiltaksleverandør hovedenhet">
              <Metadata
                header="Tiltaksleverandør hovedenhet"
                verdi={[avtale.leverandor.navn, avtale.leverandor.organisasjonsnummer]
                  .filter(Boolean)
                  .join(" - ")}
              />
            </Bolk>
          ) : null}

          {arrangor ? (
            <Bolk aria-label="Arrangør underenhet">
              <Metadata
                header="Arrangør underenhet"
                verdi={
                  arrangor.navn
                    ? `${arrangor.navn} - ${arrangor.organisasjonsnummer}`
                    : arrangor.organisasjonsnummer
                }
              />
            </Bolk>
          ) : null}

          {arrangor.kontaktperson && (
            <Metadata
              header="Kontaktperson hos arrangør"
              verdi={
                <div className={styles.leverandor_kontaktinfo}>
                  <label>{arrangor.kontaktperson.navn}</label>
                  <label>{arrangor.kontaktperson.telefon}</label>
                  <a href={`mailto:${arrangor.kontaktperson.epost}`}>
                    {arrangor.kontaktperson.epost}
                  </a>
                  {arrangor.kontaktperson.beskrivelse && (
                    <label>{arrangor.kontaktperson.beskrivelse}</label>
                  )}
                </div>
              }
            />
          )}
          {isTiltakMedFellesOppstart(tiltakstype.arenaKode) ? (
            <>
              <Separator />
              <Bolk aria-label="Fremmøte">
                <Metadata
                  header="Fremmøte tidspunkt"
                  verdi={
                    <div>
                      {`${formaterDato(
                        fremmoteDatoFromTidspunkt(fremmoteTidspunkt),
                        "Ikke satt",
                      )} ${fremmoteTidFromTidspunkt(fremmoteTidspunkt) ?? ""}`}
                    </div>
                  }
                />
              </Bolk>
              <Bolk aria-label="Antall plasser">
                <Metadata header="Fremmøte sted" verdi={fremmoteSted ?? "Ikke satt"} />
              </Bolk>
            </>
          ) : stedForGjennomforing ? (
            <>
              <Separator />
              <Bolk aria-label="Sted for gjennomføringen">
                <Metadata header="Sted for gjennomføringen" verdi={stedForGjennomforing} />
              </Bolk>
            </>
          ) : null}
        </div>
      </div>
    </>
  );
}
