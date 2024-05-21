import { useAvtale } from "@/api/avtaler/useAvtale";
import { Fragment, useRef } from "react";
import { Laster } from "@/components/laster/Laster";
import { Alert, Button, Heading, HelpText, VStack } from "@navikt/ds-react";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { NavEnhet, Opphav } from "mulighetsrommet-api-client";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import styles from "../DetaljerInfo.module.scss";
import { avtaletypeTilTekst, formaterDato } from "@/utils/Utils";
import { erAnskaffetTiltak } from "@/utils/tiltakskoder";
import { NOM_ANSATT_SIDE } from "mulighetsrommet-frontend-common/constants";
import { getDisplayName } from "@/api/enhet/helpers";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { AvbrytAvtaleModal } from "@/components/modal/AvbrytAvtaleModal";
import { Link } from "react-router-dom";

export function AvtaleDetaljer() {
  const { data: avtale, isPending, error } = useAvtale();
  const avbrytModalRef = useRef<HTMLDialogElement>(null);

  if (isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  function sorterPaRegionsnavn(a: { region: NavEnhet }, b: { region: NavEnhet }) {
    return a.region.navn.localeCompare(b.region.navn);
  }

  const {
    navn,
    avtalenummer,
    lopenummer,
    tiltakstype,
    avtaletype,
    startDato,
    sluttDato,
    administratorer,
    websaknummer,
    kontorstruktur,
    arenaAnsvarligEnhet,
    arrangor,
    opphav,
  } = avtale;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.detaljer}>
          <Bolk aria-label="Avtalenavn">
            <Metadata header={avtaletekster.avtalenavnLabel} verdi={navn} />
          </Bolk>

          <Separator />

          <Bolk aria-label="Eksterne referanser">
            {opphav === Opphav.MR_ADMIN_FLATE ? (
              <Metadata header={avtaletekster.lopenummerLabel} verdi={lopenummer} />
            ) : (
              <Metadata header={avtaletekster.arenaAvtalenummerLabel} verdi={avtalenummer} />
            )}
            {websaknummer ? (
              <Metadata header={avtaletekster.websaknummerLabel} verdi={websaknummer} />
            ) : null}
          </Bolk>

          <Separator />

          <Bolk aria-label={avtaletekster.tiltakstypeLabel}>
            <Metadata
              header={avtaletekster.tiltakstypeLabel}
              verdi={<Link to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Link>}
            />
            <Metadata
              header={avtaletekster.avtaletypeLabel}
              verdi={avtaletypeTilTekst(avtaletype)}
            />
          </Bolk>

          <Separator />

          <Heading size="small" as="h3">
            Avtalens varighet
          </Heading>

          <Bolk aria-label="Start- og sluttdato">
            <Metadata header={avtaletekster.startdatoLabel} verdi={formaterDato(startDato)} />
            <Metadata
              header={avtaletekster.sluttdatoLabel}
              verdi={sluttDato ? formaterDato(sluttDato) : "-"}
            />
          </Bolk>

          <Separator />

          <VStack gap="5">
            <Bolk aria-label={avtaletekster.prisOgBetalingLabel}>
              {erAnskaffetTiltak(tiltakstype.arenaKode) && (
                <Metadata
                  header={avtaletekster.prisOgBetalingLabel}
                  verdi={avtale.prisbetingelser ?? "-"}
                />
              )}
            </Bolk>

            {administratorer ? (
              <Bolk aria-label={avtaletekster.administratorerForAvtalenLabel}>
                <Metadata
                  header={avtaletekster.administratorerForAvtalenLabel}
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
                      avtaletekster.ingenAdministratorerSattLabel
                    )
                  }
                />
              </Bolk>
            ) : null}
          </VStack>
        </div>

        <div className={styles.detaljer}>
          {kontorstruktur.length > 1 ? (
            <Metadata
              header={avtaletekster.fylkessamarbeidLabel}
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
                  <Bolk aria-label={avtaletekster.navRegionerLabel}>
                    <Metadata
                      header={avtaletekster.navRegionerLabel}
                      verdi={struktur.region.navn}
                    />
                  </Bolk>

                  <Bolk aria-label={avtaletekster.navEnheterLabel}>
                    <Metadata
                      header={avtaletekster.navEnheterLabel}
                      verdi={
                        <ul className={styles.two_columns}>
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
                  header={avtaletekster.ansvarligEnhetFraArenaLabel}
                  verdi={getDisplayName(arenaAnsvarligEnhet)}
                />
              </dl>
              <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
                Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet når
                det opprettes avtale i Arena.
              </HelpText>
            </div>
          ) : null}

          <Separator />

          <Bolk aria-label={avtaletekster.tiltaksarrangorHovedenhetLabel}>
            <Metadata
              header={avtaletekster.tiltaksarrangorHovedenhetLabel}
              verdi={
                <Link to={`/arrangorer/${arrangor.id}`}>
                  {arrangor.navn} - {arrangor.organisasjonsnummer}
                </Link>
              }
            />
          </Bolk>

          <Bolk aria-label={avtaletekster.tiltaksarrangorUnderenheterLabel}>
            <Metadata
              header={avtaletekster.tiltaksarrangorUnderenheterLabel}
              verdi={
                <ul>
                  {arrangor.underenheter.map((enhet) => (
                    <li key={enhet.organisasjonsnummer}>
                      {`${enhet.navn} - ${enhet.organisasjonsnummer}`}
                    </li>
                  ))}
                </ul>
              }
            />
          </Bolk>

          <Separator />
          {arrangor.kontaktpersoner.length > 0 && (
            <Metadata
              header={avtaletekster.kontaktpersonerHosTiltaksarrangorLabel}
              verdi={
                <div className={styles.arrangor_kontaktinfo_container}>
                  {arrangor.kontaktpersoner.map((kontaktperson) => (
                    <ArrangorKontaktpersonDetaljer
                      key={kontaktperson.id}
                      kontaktperson={kontaktperson}
                    />
                  ))}
                </div>
              }
            />
          )}
        </div>
      </div>
      <Separator />
      <HarSkrivetilgang ressurs="Avtale">
        {avtale && avtale.status.name === "AKTIV" && (
          <Button
            size="small"
            variant="danger"
            type="button"
            onClick={() => avbrytModalRef.current?.showModal()}
          >
            Avbryt avtale
          </Button>
        )}
      </HarSkrivetilgang>
      <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />
    </>
  );
}
