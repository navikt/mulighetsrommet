import { getDisplayName } from "@/api/enhet/helpers";
import { usePollTiltaksnummer } from "@/api/gjennomforing/usePollTiltaksnummer";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { Laster } from "@/components/laster/Laster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { NokkeltallDeltakere } from "@/components/gjennomforing/NokkeltallDeltakere";
import { TiltakTilgjengeligForArrangor } from "@/components/gjennomforing/TilgjengeligTiltakForArrangor";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { Kontaktperson } from "@/pages/gjennomforing/Kontaktperson";
import { formaterDato, formatertVentetid } from "@/utils/Utils";
import { AvtaleDto, GjennomforingDto, GjennomforingOppstartstype } from "@mr/api-client";
import { useTitle } from "@mr/frontend-common";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { isKursTiltak } from "@mr/frontend-common/utils/utils";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { BodyShort, HelpText, HStack, Tag, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import styles from "./GjennomforingDetaljer.module.scss";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";

interface Props {
  gjennomforing: GjennomforingDto;
  avtale?: AvtaleDto;
}

export function GjennomforingDetaljer({ gjennomforing, avtale }: Props) {
  useTitle(`Gjennomføring ${gjennomforing.navn ? `- ${gjennomforing.navn}` : null}`);

  const navnPaaNavEnheterForKontaktperson = (enheterForKontaktperson: string[]): string => {
    return (
      gjennomforing?.navEnheter
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

  const kontaktpersonerFraNav = gjennomforing.kontaktpersoner ?? [];

  const {
    tiltakstype,
    tiltaksnummer,
    startDato,
    sluttDato,
    oppstart,
    antallPlasser,
    deltidsprosent,
    apentForPamelding,
    administratorer,
    navRegion,
    navEnheter,
    arenaAnsvarligEnhet,
    arrangor,
    stedForGjennomforing,
    amoKategorisering,
    utdanningslop,
  } = gjennomforing;

  return (
    <>
      <TwoColumnGrid separator>
        <VStack>
          <Bolk aria-label="Tiltaksnavn og tiltaksnummer" data-testid="tiltaksnavn">
            <Metadata header={gjennomforingTekster.tiltaksnavnLabel} verdi={gjennomforing.navn} />
            <Metadata
              header={gjennomforingTekster.tiltaksnummerLabel}
              verdi={tiltaksnummer ?? <HentTiltaksnummer id={gjennomforing.id} />}
            />
          </Bolk>

          <Bolk aria-label="Tiltakstype og avtaletype">
            <Metadata
              header={gjennomforingTekster.avtaleLabel}
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
                  gjennomforingTekster.ingenAvtaleForGjennomforingenLabel
                )
              }
            />
            <Metadata header={gjennomforingTekster.tiltakstypeLabel} verdi={tiltakstype.navn} />
          </Bolk>
          <Separator />
          {amoKategorisering && (
            <>
              <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />
              <Separator />
            </>
          )}
          {utdanningslop && <UtdanningslopDetaljer utdanningslop={utdanningslop} />}
          <Bolk aria-label={gjennomforingTekster.oppstartstypeLabel}>
            <Metadata
              header={gjennomforingTekster.oppstartstypeLabel}
              verdi={oppstart === GjennomforingOppstartstype.FELLES ? "Felles" : "Løpende oppstart"}
            />
          </Bolk>
          <Bolk aria-label="Start- og sluttdato">
            <Metadata
              header={gjennomforingTekster.startdatoLabel}
              verdi={formaterDato(startDato)}
            />
            <Metadata
              header={gjennomforingTekster.sluttdatoLabel}
              verdi={sluttDato ? formaterDato(sluttDato) : "-"}
            />
          </Bolk>

          <Bolk>
            <Metadata header={gjennomforingTekster.antallPlasserLabel} verdi={antallPlasser} />
            {isKursTiltak(tiltakstype.tiltakskode) && (
              <Metadata header={gjennomforingTekster.deltidsprosentLabel} verdi={deltidsprosent} />
            )}
          </Bolk>

          <Separator />
          <Bolk aria-label={gjennomforingTekster.apentForPameldingLabel}>
            <Metadata
              header={gjennomforingTekster.apentForPameldingLabel}
              verdi={apentForPamelding ? "Ja" : "Nei"}
            />
          </Bolk>

          <Separator />

          {gjennomforing?.estimertVentetid ? (
            <>
              <Bolk aria-label={gjennomforingTekster.estimertVentetidLabel}>
                <Metadata
                  header={gjennomforingTekster.estimertVentetidLabel}
                  verdi={formatertVentetid(
                    gjennomforing.estimertVentetid.verdi,
                    gjennomforing.estimertVentetid.enhet,
                  )}
                />
              </Bolk>
              <Separator />
            </>
          ) : null}

          <Bolk aria-label={gjennomforingTekster.administratorerForGjennomforingenLabel}>
            <Metadata
              header={gjennomforingTekster.administratorerForGjennomforingenLabel}
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
                  gjennomforingTekster.ingenAdministratorerSattForGjennomforingenLabel
                )
              }
            />
          </Bolk>
        </VStack>
        <VStack>
          <Bolk aria-label={gjennomforingTekster.navRegionLabel}>
            <Metadata header={gjennomforingTekster.navRegionLabel} verdi={navRegion?.navn} />
          </Bolk>

          <Bolk aria-label={gjennomforingTekster.navEnheterKontorerLabel}>
            <Metadata
              header={gjennomforingTekster.navEnheterKontorerLabel}
              verdi={
                <ul className={styles.two_columns}>
                  {navEnheter
                    .sort((a, b) => a.navn.localeCompare(b.navn))
                    .map((enhet) => (
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
                  header={gjennomforingTekster.ansvarligEnhetFraArenaLabel}
                  verdi={getDisplayName(arenaAnsvarligEnhet)}
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
              <Bolk key={index} classez={styles.nav_kontaktpersoner}>
                <Metadata
                  header={
                    <>
                      <span style={{ fontWeight: "bold" }}>Kontaktperson for:</span>{" "}
                      <span style={{ fontWeight: "initial" }}>
                        {navnPaaNavEnheterForKontaktperson(
                          kp.navEnheter.sort((a, b) => a.localeCompare(b)),
                        )}
                      </span>
                    </>
                  }
                  verdi={<Kontaktperson kontaktperson={kp} />}
                />
              </Bolk>
            );
          })}

          <Separator />

          {avtale?.arrangor ? (
            <Bolk aria-label={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}>
              <Metadata
                header={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
                verdi={
                  <Link to={`/arrangorer/${avtale.arrangor.id}`}>
                    {avtale.arrangor.navn} - {avtale.arrangor.organisasjonsnummer}
                  </Link>
                }
              />
            </Bolk>
          ) : null}

          {arrangor ? (
            <Bolk aria-label={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}>
              <Metadata
                header={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
                verdi={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
              />
            </Bolk>
          ) : null}
          {arrangor.kontaktpersoner.length > 0 && (
            <Metadata
              header={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
              verdi={
                <VStack>
                  {arrangor.kontaktpersoner.map((kontaktperson) => (
                    <ArrangorKontaktpersonDetaljer
                      key={kontaktperson.id}
                      kontaktperson={kontaktperson}
                    />
                  ))}
                </VStack>
              }
            />
          )}
          {stedForGjennomforing && (
            <>
              <Separator />
              <Bolk aria-label={gjennomforingTekster.stedForGjennomforingLabel}>
                <Metadata
                  header={gjennomforingTekster.stedForGjennomforingLabel}
                  verdi={stedForGjennomforing}
                />
              </Bolk>
            </>
          )}
          <TiltakTilgjengeligForArrangor gjennomforing={gjennomforing} />
        </VStack>
      </TwoColumnGrid>
      <NokkeltallDeltakere gjennomforingId={gjennomforing.id} />
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
