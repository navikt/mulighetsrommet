import { getDisplayName } from "@/api/enhet/helpers";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { RegistrerteOpsjoner } from "@/components/avtaler/opsjoner/RegistrerteOpsjoner";
import { opsjonsmodellTilTekst } from "@/components/avtaler/opsjoner/opsjonsmodeller";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { avtaletypeTilTekst, formaterDato, sorterPaRegionsnavn } from "@/utils/Utils";
import { AvtaleDto, Avtaletype } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { Alert, Heading, HelpText, VStack } from "@navikt/ds-react";
import { Fragment } from "react";
import { Link } from "react-router";

interface Props {
  avtale: AvtaleDto;
  okonomiTabEnabled?: boolean;
}

export function AvtaleDetaljer({ avtale, okonomiTabEnabled }: Props) {
  const {
    navn,
    avtalenummer,
    tiltakstype,
    avtaletype,
    startDato,
    sluttDato,
    administratorer,
    sakarkivNummer,
    kontorstruktur,
    arenaAnsvarligEnhet,
    arrangor,
    amoKategorisering,
    utdanningslop,
  } = avtale;

  return (
    <TwoColumnGrid separator>
      <VStack>
        <Bolk aria-label="Avtalenavn">
          <Metadata header={avtaletekster.avtalenavnLabel} verdi={navn} />
        </Bolk>

        <Separator />

        <Bolk aria-label="Eksterne referanser">
          <Metadata header={avtaletekster.avtalenummerLabel} verdi={avtalenummer} />
          {sakarkivNummer ? (
            <Metadata header={avtaletekster.sakarkivNummerLabel} verdi={sakarkivNummer} />
          ) : null}
        </Bolk>

        <Separator />

        <Bolk aria-label={avtaletekster.tiltakstypeLabel}>
          <Metadata
            header={avtaletekster.tiltakstypeLabel}
            verdi={<Link to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Link>}
          />
          <Metadata header={avtaletekster.avtaletypeLabel} verdi={avtaletypeTilTekst(avtaletype)} />
        </Bolk>
        <Separator />
        {amoKategorisering && (
          <>
            <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />
            <Separator />
          </>
        )}

        {utdanningslop ? <UtdanningslopDetaljer utdanningslop={utdanningslop} /> : null}

        <Heading size="small" as="h3">
          Avtalens varighet
        </Heading>

        {avtale?.opsjonsmodellData?.opsjonsmodell &&
        avtale.avtaletype !== Avtaletype.FORHANDSGODKJENT ? (
          <>
            <Bolk aria-label="Opsjonsmodell">
              <Metadata
                header={avtaletekster.avtaltForlengelseLabel}
                verdi={opsjonsmodellTilTekst(avtale?.opsjonsmodellData)}
              />
            </Bolk>
          </>
        ) : null}

        <Bolk aria-label="Start- og sluttdato">
          <Metadata header={avtaletekster.startdatoLabel} verdi={formaterDato(startDato)} />
          <Metadata
            header={avtaletekster.sluttdatoLabel(avtale.opsjonerRegistrert.length > 0)}
            verdi={sluttDato ? formaterDato(sluttDato) : "-"}
          />
          {avtale?.opsjonsmodellData?.opsjonMaksVarighet ? (
            <Metadata
              header={avtaletekster.maksVarighetLabel}
              verdi={
                avtale.opsjonsmodellData.opsjonMaksVarighet
                  ? formaterDato(avtale.opsjonsmodellData.opsjonMaksVarighet)
                  : "-"
              }
            />
          ) : null}
        </Bolk>

        {avtale.opsjonerRegistrert.length > 0 ? (
          <RegistrerteOpsjoner readOnly avtale={avtale} />
        ) : null}

        <Separator />

        <VStack gap="5">
          <Bolk aria-label={avtaletekster.prisOgBetalingLabel}>
            {okonomiTabEnabled === false && (
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
                            <Lenke to={`${NOM_ANSATT_SIDE}${admin.navIdent}`} isExternal>
                              {`${admin.navn} - ${admin.navIdent}`}{" "}
                            </Lenke>
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
      </VStack>
      <VStack>
        {kontorstruktur.length > 1 ? (
          <Bolk>
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
          </Bolk>
        ) : (
          kontorstruktur.map((struktur, index) => {
            return (
              <Fragment key={index}>
                <Bolk aria-label={avtaletekster.navRegionerLabel}>
                  <Metadata header={avtaletekster.navRegionerLabel} verdi={struktur.region.navn} />
                </Bolk>

                <Bolk aria-label={avtaletekster.navEnheterLabel}>
                  <Metadata
                    header={avtaletekster.navEnheterLabel}
                    verdi={
                      <ul className="columns-2">
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
        {arrangor ? (
          <VStack gap="5">
            <Metadata
              header={avtaletekster.tiltaksarrangorHovedenhetLabel}
              verdi={
                <Link to={`/arrangorer/${arrangor.id}`}>
                  {arrangor.navn} - {arrangor.organisasjonsnummer}
                </Link>
              }
            />

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
          </VStack>
        ) : (
          <AvtaleErUtkastOgArrangorManglerMelding />
        )}

        <Separator />
        {arrangor && arrangor.kontaktpersoner.length > 0 && (
          <Metadata
            header={avtaletekster.kontaktpersonerHosTiltaksarrangorLabel}
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
      </VStack>
    </TwoColumnGrid>
  );
}

export const AvtaleErUtkastOgArrangorManglerMelding = () => {
  return <Alert variant="warning">Arrangør mangler</Alert>;
};
