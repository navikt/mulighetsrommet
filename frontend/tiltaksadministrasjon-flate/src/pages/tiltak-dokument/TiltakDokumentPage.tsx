import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { BodyShort, Heading, VStack } from "@navikt/ds-react";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakDokument } from "@/api/tiltak-dokument/useTiltakDokument";
import { Bolk } from "@/components/detaljside/Bolk";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { Kontaktperson } from "@/pages/gjennomforing/Kontaktperson";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { RedaksjoneltInnhold } from "@/components/redaksjoneltInnhold/RedaksjoneltInnhold";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { CaretDownFillIcon, CaretUpFillIcon } from "@navikt/aksel-icons";
import { Suspense, useState } from "react";
import { Kontorstruktur } from "@tiltaksadministrasjon/api-client";
import { TiltakDokumentHandlinger } from "@/components/tiltak-dokument/TiltakDokumentHandlinger";
import { Laster } from "@/components/laster/Laster";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";

export function TiltakDokumentPage() {
  const { tiltakDokumentId } = useRequiredParams(["tiltakDokumentId"]);
  const { data: tiltakDokument } = useTiltakDokument(tiltakDokumentId);
  const { data: ansatt } = useHentAnsatt();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltaksdokumenter", lenke: "/tiltak-dokumenter" },
    { tittel: tiltakDokument.navn },
  ];

  return (
    <>
      <title>{`Tiltaksdokument | ${tiltakDokument.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakDokumentIkon />} heading={tiltakDokument.navn} />
      <ContentBox>
        <WhitePaddedBox>
          <InlineErrorBoundary>
            <Suspense fallback={<Laster tekst="Laster handlinger..." />}>
              <TiltakDokumentHandlinger ansatt={ansatt} tiltakDokument={tiltakDokument} />
            </Suspense>
          </InlineErrorBoundary>
          <Separator />
          <VStack gap="space-16">
            <Bolk aria-label="Grunninfo">
              <VStack gap="space-8">
                <div>
                  <Heading size="small" level="2">
                    Tiltakstype
                  </Heading>
                  <BodyShort>{tiltakDokument.tiltakstype.navn}</BodyShort>
                </div>
                {tiltakDokument.stedForGjennomforing && (
                  <div>
                    <Heading size="small" level="2">
                      Sted for gjennomføring
                    </Heading>
                    <BodyShort>{tiltakDokument.stedForGjennomforing}</BodyShort>
                  </div>
                )}
                {tiltakDokument.arrangor && (
                  <div>
                    <Heading size="small" level="2">
                      Arrangør
                    </Heading>
                    <BodyShort>
                      {tiltakDokument.arrangor.navn} — {tiltakDokument.arrangor.organisasjonsnummer}
                    </BodyShort>
                    {tiltakDokument.arrangorKontaktpersoner.length > 0 && (
                      <VStack gap="space-4" className="mt-2">
                        <Heading size="xsmall" level="3">
                          {gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
                        </Heading>
                        {tiltakDokument.arrangorKontaktpersoner.map((kp) => (
                          <ArrangorKontaktpersonDetaljer key={kp.id} kontaktperson={kp} />
                        ))}
                      </VStack>
                    )}
                  </div>
                )}
                {tiltakDokument.administratorer.length > 0 && (
                  <div>
                    <Heading size="small" level="2">
                      Administratorer
                    </Heading>
                    <VStack gap="space-4">
                      {tiltakDokument.administratorer.map((admin) => (
                        <BodyShort key={admin.navIdent}>
                          {admin.navn} ({admin.navIdent})
                        </BodyShort>
                      ))}
                    </VStack>
                  </div>
                )}
              </VStack>
            </Bolk>

            <Separator />

            <TwoColumnGrid separator>
              <RedaksjoneltInnhold
                beskrivelse={tiltakDokument.beskrivelse ?? null}
                faneinnhold={tiltakDokument.faneinnhold ?? null}
              />
              <RedaksjoneltInnholdContainer>
                {tiltakDokument.kontorstruktur.length > 0 && (
                  <Bolk aria-label={gjennomforingTekster.tilgjengeligIModiaLabel}>
                    <MetadataVStack
                      label={gjennomforingTekster.tilgjengeligIModiaLabel}
                      value={
                        <RegionOgUnderenheter kontorstruktur={tiltakDokument.kontorstruktur} />
                      }
                    />
                  </Bolk>
                )}
                {tiltakDokument.kontaktpersoner.length > 0 && (
                  <Bolk>
                    <MetadataVStack
                      label={gjennomforingTekster.kontaktpersonNav.mainLabel}
                      value={
                        <VStack gap="space-8">
                          {tiltakDokument.kontaktpersoner.map((kp, index) => (
                            <Kontaktperson key={index} kontaktperson={kp} />
                          ))}
                        </VStack>
                      }
                    />
                  </Bolk>
                )}
              </RedaksjoneltInnholdContainer>
            </TwoColumnGrid>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}

function RegionOgUnderenheter({ kontorstruktur }: { kontorstruktur: Kontorstruktur[] }) {
  const [openRegions, setOpenRegions] = useState<string[]>([]);

  const toggleRegion = (enhetsnummer: string) => {
    setOpenRegions((prev) =>
      prev.includes(enhetsnummer)
        ? prev.filter((num) => num !== enhetsnummer)
        : [...prev, enhetsnummer],
    );
  };

  return (
    <ul>
      {kontorstruktur.map((kontor) => (
        <li className="font-ax-bold my-2 ml-3" key={kontor.region.enhetsnummer}>
          <button
            className="hover:cursor-pointer flex"
            onClick={() => toggleRegion(kontor.region.enhetsnummer)}
            title={`${kontor.region.navn} (${kontor.kontorer.length} kontorer)`}
          >
            {kontor.region.navn} ({kontor.kontorer.length || 0})
            {openRegions.includes(kontor.region.enhetsnummer) ? (
              <CaretUpFillIcon className="text-xl" />
            ) : (
              <CaretDownFillIcon className="text-xl" />
            )}
          </button>
          {openRegions.includes(kontor.region.enhetsnummer) && (
            <ul className="list-disc ml-5">
              {kontor.kontorer.map((k) => (
                <li className="ml-5 font-thin" key={k.enhetsnummer}>
                  {k.navn}
                </li>
              ))}
            </ul>
          )}
        </li>
      ))}
    </ul>
  );
}
