import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { getDisplayName } from "@/api/enhet/helpers";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { usePollTiltaksnummer } from "@/api/gjennomforing/usePollTiltaksnummer";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { NokkeltallDeltakere } from "@/components/gjennomforing/NokkeltallDeltakere";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { TiltakTilgjengeligForArrangor } from "@/components/gjennomforing/TilgjengeligTiltakForArrangor";
import { Laster } from "@/components/laster/Laster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { Kontaktperson } from "@/pages/gjennomforing/Kontaktperson";
import { formatertVentetid, isKursTiltak } from "@/utils/Utils";
import { GjennomforingOppstartstype, Kontorstruktur } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { CaretDownFillIcon, CaretUpFillIcon } from "@navikt/aksel-icons";
import { BodyShort, HelpText, HStack, Tag, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { Link, useParams } from "react-router";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";

function useGjennomforingInfoData() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);

  return {
    gjennomforing,
    ansatt,
    avtale,
  };
}

export function GjennomforingDetaljer() {
  const { gjennomforing, avtale, ansatt } = useGjennomforingInfoData();
  const kontorer = gjennomforing.kontorstruktur.flatMap((struktur) => struktur.kontorer);
  const navnPaaNavEnheterForKontaktperson = (enheterForKontaktperson: string[]): string => {
    return (
      kontorer
        .map((kontor) => {
          return enheterForKontaktperson
            .map((kp) => {
              if (kontor.enhetsnummer === kp) {
                return kontor.navn;
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
    kontorstruktur,
    arenaAnsvarligEnhet,
    arrangor,
    stedForGjennomforing,
    amoKategorisering,
    utdanningslop,
  } = gjennomforing;

  return (
    <>
      <GjennomforingKnapperad ansatt={ansatt} avtale={avtale} gjennomforing={gjennomforing} />
      <TwoColumnGrid separator>
        <VStack justify={"space-between"}>
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
          {gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable gjennomforing={gjennomforing} readOnly />
          )}

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
                          <Lenke
                            target="_blank"
                            rel="noopener noreferrer"
                            to={`${NOM_ANSATT_SIDE}${admin?.navIdent}`}
                            isExternal
                            className="flex gap-1.5"
                          >
                            {`${admin?.navn} - ${admin?.navIdent}`}{" "}
                          </Lenke>
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
          <Bolk aria-label={gjennomforingTekster.tilgjengeligIModiaLabel}>
            <Metadata
              header={gjennomforingTekster.tilgjengeligIModiaLabel}
              verdi={<RegionOgLokalkontorer kontorstruktur={kontorstruktur} />}
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
              <Bolk key={index}>
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
          <VStack gap="5">
            {avtale?.arrangor ? (
              <Metadata
                header={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
                verdi={
                  <Link to={`/arrangorer/${avtale.arrangor.id}`}>
                    {avtale.arrangor.navn} - {avtale.arrangor.organisasjonsnummer}
                  </Link>
                }
              />
            ) : null}

            {arrangor ? (
              <Metadata
                header={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
                verdi={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
              />
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
                <Metadata
                  header={gjennomforingTekster.stedForGjennomforingLabel}
                  verdi={stedForGjennomforing}
                />
              </>
            )}
          </VStack>
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

function RegionOgLokalkontorer({ kontorstruktur }: { kontorstruktur: Kontorstruktur }) {
  const [openRegions, setOpenRegions] = useState<string[]>([]);

  const toggleRegion = (enhetsnummer: string) => {
    setOpenRegions((prev) =>
      prev.includes(enhetsnummer)
        ? prev.filter((num) => num !== enhetsnummer)
        : [...prev, enhetsnummer],
    );
  };

  function isRegionOpen(enhetsnummer: string) {
    return openRegions.includes(enhetsnummer);
  }

  return (
    <ul>
      {kontorstruktur.map((kontor) => {
        return (
          <li className="font-bold my-2 ml-3" key={kontor.region.enhetsnummer}>
            <button
              className="hover:cursor-pointer flex"
              onClick={() => toggleRegion(kontor.region.enhetsnummer)}
              title={`${kontor.region.navn} (${kontor.kontorer.length} kontorer)`}
            >
              {kontor.region.navn} ({kontor.kontorer.length || 0})
              {isRegionOpen(kontor.region.enhetsnummer) ? (
                <CaretUpFillIcon className="text-xl" />
              ) : (
                <CaretDownFillIcon className="text-xl" />
              )}
            </button>
            {isRegionOpen(kontor.region.enhetsnummer) && (
              <ul className="list-disc ml-5">
                {kontor.kontorer.map((kontor) => (
                  <li className="ml-5 font-thin" key={kontor.enhetsnummer}>
                    {kontor.navn}
                  </li>
                ))}
              </ul>
            )}
          </li>
        );
      })}
    </ul>
  );
}
