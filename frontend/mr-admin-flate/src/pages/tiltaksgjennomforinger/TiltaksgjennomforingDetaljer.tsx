import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { BodyShort, HelpText, Tag } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
  VirksomhetKontaktperson,
} from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";
import { Link } from "react-router-dom";
import { usePollTiltaksnummer } from "../../api/tiltaksgjennomforing/usePollTiltaksnummer";
import { Bolk } from "../../components/detaljside/Bolk";
import { Metadata, Separator } from "../../components/detaljside/Metadata";
import { Laster } from "../../components/laster/Laster";
import { formaterDato, formatertVentetid } from "../../utils/Utils";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import styles from "../DetaljerInfo.module.scss";
import { Kontaktperson } from "./Kontaktperson";

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

  const kontaktpersonerFraNav = tiltaksgjennomforing.kontaktpersoner ?? [];

  const {
    tiltakstype,
    tiltaksnummer,
    startDato,
    sluttDato,
    oppstart,
    antallPlasser,
    deltidsprosent,
    apentForInnsok,
    administratorer,
    navRegion,
    navEnheter,
    arenaAnsvarligEnhet,
    arrangor,
    stedForGjennomforing,
  } = tiltaksgjennomforing;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.detaljer}>
          <Bolk aria-label="Tiltaksnavn og tiltaksnummer" data-testid="tiltaksnavn">
            <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.navn} />
            <Metadata
              header="Tiltaksnummer"
              verdi={tiltaksnummer ?? <HentTiltaksnummer id={tiltaksgjennomforing.id} />}
            />
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

          <Bolk aria-label="Oppstartsdato">
            <Metadata
              header="Oppstartstype"
              verdi={
                oppstart === TiltaksgjennomforingOppstartstype.FELLES
                  ? "Felles"
                  : "Løpende oppstart"
              }
            />
          </Bolk>
          <Bolk aria-label="Start- og sluttdato">
            <Metadata header="Startdato" verdi={formaterDato(startDato)} />
            <Metadata header="Sluttdato" verdi={sluttDato ? formaterDato(sluttDato) : "-"} />
          </Bolk>

          <Bolk>
            <Metadata header="Antall plasser" verdi={antallPlasser} />
            {isTiltakMedFellesOppstart(tiltakstype.arenaKode) && (
              <Metadata header="Deltidsprosent" verdi={deltidsprosent} />
            )}
          </Bolk>

          {apentForInnsok ? (
            <>
              <Separator />
              <Bolk aria-label="Åpent for innsøk">
                <Metadata header="Åpent for innsøk" verdi={apentForInnsok ? "Ja" : "Nei"} />
              </Bolk>
            </>
          ) : null}

          <Separator />

          {tiltaksgjennomforing?.estimertVentetid ? (
            <>
              <Bolk aria-label="Estimert ventetid">
                <Metadata
                  header="Estimert ventetid"
                  verdi={formatertVentetid(
                    tiltaksgjennomforing.estimertVentetid.verdi,
                    tiltaksgjennomforing.estimertVentetid.enhet,
                  )}
                />
              </Bolk>
              <Separator />
            </>
          ) : null}

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
            <Bolk aria-label="Tiltaksarrangør hovedenhet">
              <Metadata
                header="Tiltaksarrangør hovedenhet"
                verdi={[avtale.leverandor.navn, avtale.leverandor.organisasjonsnummer]
                  .filter(Boolean)
                  .join(" - ")}
              />
            </Bolk>
          ) : null}

          {arrangor ? (
            <Bolk aria-label="Tiltaksarrangør underenhet">
              <Metadata
                header="Tiltaksarrangør underenhet"
                verdi={
                  arrangor.navn
                    ? `${arrangor.navn} - ${arrangor.organisasjonsnummer}`
                    : arrangor.organisasjonsnummer
                }
              />
            </Bolk>
          ) : null}
          {arrangor.kontaktpersoner.length > 0 && (
            <Metadata
              header="Kontaktpersoner hos tiltaksarrangør"
              verdi={
                <div className={styles.leverandor_kontaktinfo_container}>
                  {arrangor.kontaktpersoner.map((person: VirksomhetKontaktperson) => (
                    <div key={person.id} className={styles.leverandor_kontaktinfo}>
                      <BodyShort>{person.navn}</BodyShort>
                      <BodyShort>{person.telefon}</BodyShort>
                      <a href={`mailto:${person.epost}`}>{person.epost}</a>
                      {person.beskrivelse && <BodyShort>{person.beskrivelse}</BodyShort>}
                    </div>
                  ))}
                </div>
              }
            />
          )}
          {stedForGjennomforing && (
            <>
              <Separator />
              <Bolk aria-label="Sted for gjennomføring">
                <Metadata header="Sted for gjennomføring" verdi={stedForGjennomforing} />
              </Bolk>
            </>
          )}
        </div>
      </div>
    </>
  );
}

function HentTiltaksnummer({ id }: { id: string }) {
  const { isError, isLoading, data } = usePollTiltaksnummer(id);
  return isError ? (
    <Tag variant="error">Klarte ikke hente tiltaksnummer</Tag>
  ) : isLoading ? (
    <Laster />
  ) : (
    data?.tiltaksnummer
  );
}
