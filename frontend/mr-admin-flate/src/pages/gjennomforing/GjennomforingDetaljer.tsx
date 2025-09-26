import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { getDisplayName } from "@/api/enhet/helpers";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
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
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { formatertVentetid, isKursTiltak } from "@/utils/Utils";
import { GjennomforingOppstartstype } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { BodyShort, HelpText, HStack, Tag, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import { GjennomforingPageLayout } from "./GjennomforingPageLayout";

export function GjennomforingDetaljer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);

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
    arenaAnsvarligEnhet,
    arrangor,
    stedForGjennomforing,
    amoKategorisering,
    utdanningslop,
  } = gjennomforing;

  return (
    <GjennomforingPageLayout>
      <TwoColumnGrid separator>
        <VStack justify={"space-between"}>
          <Bolk aria-label="Tiltaksnavn og tiltaksnummer" data-testid="tiltaksnavn">
            <Metadata header={gjennomforingTekster.tiltaksnavnLabel} value={gjennomforing.navn} />
            <Metadata
              header={gjennomforingTekster.tiltaksnummerLabel}
              value={tiltaksnummer ?? <HentTiltaksnummer id={gjennomforing.id} />}
            />
          </Bolk>

          <Bolk aria-label="Tiltakstype og avtaletype">
            <Metadata
              header={gjennomforingTekster.avtaleLabel}
              value={
                avtale?.id ? (
                  <>
                    <Link to={`/avtaler/${avtale.id}`}>
                      {avtale.navn} {avtale.avtalenummer ? ` - ${avtale.avtalenummer}` : null}
                    </Link>{" "}
                    <BodyShort>
                      <small>
                        Avtalens periode: {formaterDato(avtale.startDato)} -{" "}
                        {avtale.sluttDato ? formaterDato(avtale.sluttDato) : ""}
                      </small>
                    </BodyShort>
                  </>
                ) : (
                  gjennomforingTekster.ingenAvtaleForGjennomforingenLabel
                )
              }
            />
            <Metadata header={gjennomforingTekster.tiltakstypeLabel} value={tiltakstype.navn} />
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
              value={oppstart === GjennomforingOppstartstype.FELLES ? "Felles" : "Løpende oppstart"}
            />
          </Bolk>
          <Bolk aria-label="Start- og sluttdato">
            <Metadata
              header={gjennomforingTekster.startdatoLabel}
              value={formaterDato(startDato)}
            />
            <Metadata
              header={gjennomforingTekster.sluttdatoLabel}
              value={sluttDato ? formaterDato(sluttDato) : "-"}
            />
          </Bolk>
          {gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable gjennomforing={gjennomforing} readOnly />
          )}

          <Bolk>
            <Metadata header={gjennomforingTekster.antallPlasserLabel} value={antallPlasser} />
            {isKursTiltak(tiltakstype.tiltakskode) && (
              <Metadata header={gjennomforingTekster.deltidsprosentLabel} value={deltidsprosent} />
            )}
          </Bolk>

          <Separator />
          <Bolk aria-label={gjennomforingTekster.apentForPameldingLabel}>
            <Metadata
              header={gjennomforingTekster.apentForPameldingLabel}
              value={apentForPamelding ? "Ja" : "Nei"}
            />
          </Bolk>

          <Separator />

          {gjennomforing.estimertVentetid ? (
            <>
              <Bolk aria-label={gjennomforingTekster.estimertVentetidLabel}>
                <Metadata
                  header={gjennomforingTekster.estimertVentetidLabel}
                  value={formatertVentetid(
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
              value={
                administratorer.length ? (
                  <ul>
                    {administratorer.map((admin) => {
                      return (
                        <li key={admin.navIdent}>
                          <Lenke
                            target="_blank"
                            rel="noopener noreferrer"
                            to={`${NOM_ANSATT_SIDE}${admin.navIdent}`}
                            isExternal
                            className="flex gap-1.5"
                          >
                            {`${admin.navn} - ${admin.navIdent}`}{" "}
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
          {arenaAnsvarligEnhet ? (
            <Bolk>
              <div style={{ display: "flex", gap: "1rem" }}>
                <Metadata
                  header={gjennomforingTekster.ansvarligEnhetFraArenaLabel}
                  value={getDisplayName(arenaAnsvarligEnhet)}
                />
                <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
                  Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet
                  når man oppretter tiltak i Arena.
                </HelpText>
              </div>
            </Bolk>
          ) : null}
          <Separator />
          <VStack gap="5">
            {avtale?.arrangor ? (
              <Metadata
                header={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
                value={
                  <Link to={`/arrangorer/${avtale.arrangor.id}`}>
                    {avtale.arrangor.navn} - {avtale.arrangor.organisasjonsnummer}
                  </Link>
                }
              />
            ) : null}
            <Metadata
              header={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
              value={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
            />
            {arrangor.kontaktpersoner.length > 0 && (
              <Metadata
                header={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
                value={
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
                  value={stedForGjennomforing}
                />
              </>
            )}
          </VStack>
          {new Date() < new Date(gjennomforing.startDato) && (
            <TiltakTilgjengeligForArrangor gjennomforing={gjennomforing} />
          )}
        </VStack>
      </TwoColumnGrid>
      <NokkeltallDeltakere gjennomforingId={gjennomforing.id} />
    </GjennomforingPageLayout>
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
