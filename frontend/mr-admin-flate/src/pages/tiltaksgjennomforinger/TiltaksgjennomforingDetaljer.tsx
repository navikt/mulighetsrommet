import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { BodyShort, HStack, HelpText, Tag } from "@navikt/ds-react";
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
import { tiltaktekster } from "../../components/ledetekster/tiltaksgjennomforingLedetekster";

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
            <Metadata header={tiltaktekster.tiltaksnavnLabel} verdi={tiltaksgjennomforing.navn} />
            <Metadata
              header={tiltaktekster.tiltaksnummerLabel}
              verdi={tiltaksnummer ?? <HentTiltaksnummer id={tiltaksgjennomforing.id} />}
            />
          </Bolk>

          <Bolk aria-label="Tiltakstype og avtaletype">
            <Metadata
              header={tiltaktekster.avtaleLabel}
              verdi={
                avtale?.id ? (
                  <>
                    <Link to={`/avtaler/${avtale.id}`}>
                      {avtale.navn} {avtale.avtalenummer ? ` - ${avtale.avtalenummer}` : null}
                    </Link>{" "}
                    <BodyShort>
                      <small>
                        Avtalens periode: {formaterDato(avtale.startDato)} -{" "}
                        {avtale?.sluttDato ? formaterDato(avtale.sluttDato) : ""}
                      </small>
                    </BodyShort>
                  </>
                ) : (
                  tiltaktekster.ingenAvtaleForGjennomforingenLabel
                )
              }
            />
            <Metadata header={tiltaktekster.tiltakstypeLabel} verdi={tiltakstype.navn} />
          </Bolk>

          <Separator />

          <Bolk aria-label={tiltaktekster.oppstartstypeLabel}>
            <Metadata
              header={tiltaktekster.oppstartstypeLabel}
              verdi={
                oppstart === TiltaksgjennomforingOppstartstype.FELLES
                  ? "Felles"
                  : "Løpende oppstart"
              }
            />
          </Bolk>
          <Bolk aria-label="Start- og sluttdato">
            <Metadata header={tiltaktekster.startdatoLabel} verdi={formaterDato(startDato)} />
            <Metadata
              header={tiltaktekster.sluttdatoLabel}
              verdi={sluttDato ? formaterDato(sluttDato) : "-"}
            />
          </Bolk>

          <Bolk>
            <Metadata header={tiltaktekster.antallPlasserLabel} verdi={antallPlasser} />
            {isTiltakMedFellesOppstart(tiltakstype.arenaKode) && (
              <Metadata header={tiltaktekster.deltidsprosentLabel} verdi={deltidsprosent} />
            )}
          </Bolk>

          <Separator />
          <Bolk aria-label={tiltaktekster.apentForInnsokLabel}>
            <Metadata
              header={tiltaktekster.apentForInnsokLabel}
              verdi={apentForInnsok ? "Ja" : "Nei"}
            />
          </Bolk>

          <Separator />

          {tiltaksgjennomforing?.estimertVentetid ? (
            <>
              <Bolk aria-label={tiltaktekster.estimertVentetidLabel}>
                <Metadata
                  header={tiltaktekster.estimertVentetidLabel}
                  verdi={formatertVentetid(
                    tiltaksgjennomforing.estimertVentetid.verdi,
                    tiltaksgjennomforing.estimertVentetid.enhet,
                  )}
                />
              </Bolk>
              <Separator />
            </>
          ) : null}

          <Bolk aria-label={tiltaktekster.administratorerForGjennomforingenLabel}>
            <Metadata
              header={tiltaktekster.administratorerForGjennomforingenLabel}
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
                  tiltaktekster.ingenAdministratorerSattForGjennomforingenLabel
                )
              }
            />
          </Bolk>
        </div>

        <div className={styles.detaljer}>
          <Bolk aria-label={tiltaktekster.navRegionLabel}>
            <Metadata header={tiltaktekster.navRegionLabel} verdi={navRegion?.navn} />
          </Bolk>

          <Bolk aria-label={tiltaktekster.navEnheterKontorerLabel}>
            <Metadata
              header={tiltaktekster.navEnheterKontorerLabel}
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
                  header={tiltaktekster.ansvarligEnhetFraArenaLabel}
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
                aria-label={tiltaktekster.kontaktpersonNavEnheter(
                  navnPaaNavEnheterForKontaktperson(kp.navEnheter),
                )}
                key={index}
              >
                <Metadata
                  header={tiltaktekster.kontaktpersonNavEnheter(
                    navnPaaNavEnheterForKontaktperson(kp.navEnheter),
                  )}
                  verdi={<Kontaktperson kontaktperson={kp} />}
                />
              </Bolk>
            );
          })}

          <Separator />

          {avtale?.leverandor ? (
            <Bolk aria-label={tiltaktekster.tiltaksarrangorHovedenhetLabel}>
              <Metadata
                header={tiltaktekster.tiltaksarrangorHovedenhetLabel}
                verdi={[avtale.leverandor.navn, avtale.leverandor.organisasjonsnummer]
                  .filter(Boolean)
                  .join(" - ")}
              />
            </Bolk>
          ) : null}

          {arrangor ? (
            <Bolk aria-label={tiltaktekster.tiltaksarrangorUnderenhetLabel}>
              <Metadata
                header={tiltaktekster.tiltaksarrangorUnderenhetLabel}
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
              header={tiltaktekster.kontaktpersonerHosTiltaksarrangorLabel}
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
              <Bolk aria-label={tiltaktekster.stedForGjennomforingLabel}>
                <Metadata
                  header={tiltaktekster.stedForGjennomforingLabel}
                  verdi={stedForGjennomforing}
                />
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
    <HStack align={"center"} gap="1">
      <Laster />
      <span>Henter tiltaksnummer i Arena</span>
    </HStack>
  ) : (
    data?.tiltaksnummer
  );
}
