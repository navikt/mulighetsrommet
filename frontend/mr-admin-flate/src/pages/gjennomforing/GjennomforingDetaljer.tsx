import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { getDisplayName } from "@/api/enhet/helpers";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { usePollTiltaksnummer } from "@/api/gjennomforing/usePollTiltaksnummer";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { Bolk } from "@/components/detaljside/Bolk";
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
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { BodyShort, HelpText, HStack, Tag, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import { GjennomforingPageLayout } from "./GjennomforingPageLayout";
import { GjennomforingOppstartstype } from "@tiltaksadministrasjon/api-client";
import { Metadata, Separator } from "@mr/frontend-common/components/datadriven/Metadata";

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
    oppmoteSted,
    amoKategorisering,
    utdanningslop,
  } = gjennomforing;

  return (
    <GjennomforingPageLayout>
      <TwoColumnGrid separator>
        <VStack justify={"space-between"}>
          <Bolk aria-label="Tiltaksnavn og tiltaksnummer" data-testid="tiltaksnavn">
            <Metadata label={gjennomforingTekster.tiltaksnavnLabel} value={gjennomforing.navn} />
            <Metadata
              label={gjennomforingTekster.tiltaksnummerLabel}
              value={tiltaksnummer ?? <HentTiltaksnummer id={gjennomforing.id} />}
            />
          </Bolk>

          <Bolk aria-label="Tiltakstype og avtaletype">
            <Metadata
              label={gjennomforingTekster.avtaleLabel}
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
            <Metadata label={gjennomforingTekster.tiltakstypeLabel} value={tiltakstype.navn} />
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
              label={gjennomforingTekster.oppstartstypeLabel}
              value={oppstart === GjennomforingOppstartstype.FELLES ? "Felles" : "Løpende oppstart"}
            />
          </Bolk>
          <Bolk aria-label="Start- og sluttdato">
            <Metadata label={gjennomforingTekster.startdatoLabel} value={formaterDato(startDato)} />
            <Metadata
              label={gjennomforingTekster.sluttdatoLabel}
              value={sluttDato ? formaterDato(sluttDato) : "-"}
            />
          </Bolk>
          {gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable gjennomforing={gjennomforing} readOnly />
          )}

          <Bolk>
            <Metadata label={gjennomforingTekster.antallPlasserLabel} value={antallPlasser} />
            {isKursTiltak(tiltakstype.tiltakskode) && (
              <Metadata label={gjennomforingTekster.deltidsprosentLabel} value={deltidsprosent} />
            )}
          </Bolk>

          <Separator />
          <Bolk aria-label={gjennomforingTekster.apentForPameldingLabel}>
            <Metadata
              label={gjennomforingTekster.apentForPameldingLabel}
              value={apentForPamelding ? "Ja" : "Nei"}
            />
          </Bolk>

          <Separator />

          {gjennomforing.estimertVentetid ? (
            <>
              <Bolk aria-label={gjennomforingTekster.estimertVentetidLabel}>
                <Metadata
                  label={gjennomforingTekster.estimertVentetidLabel}
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
              label={gjennomforingTekster.administratorerForGjennomforingenLabel}
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
                  label={gjennomforingTekster.ansvarligEnhetFraArenaLabel}
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
                label={gjennomforingTekster.tiltaksarrangorHovedenhetLabel}
                value={
                  <Link to={`/arrangorer/${avtale.arrangor.id}`}>
                    {avtale.arrangor.navn} - {avtale.arrangor.organisasjonsnummer}
                  </Link>
                }
              />
            ) : null}
            <Metadata
              label={gjennomforingTekster.tiltaksarrangorUnderenhetLabel}
              value={`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
            />
            {arrangor.kontaktpersoner.length > 0 && (
              <Metadata
                label={gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
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
            {(stedForGjennomforing || oppmoteSted) && (
              <>
                <Separator />
                <Metadata
                  label={gjennomforingTekster.stedForGjennomforingLabel}
                  value={stedForGjennomforing}
                />
                <Metadata label={gjennomforingTekster.oppmoteStedLabel} value={oppmoteSted} />
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
