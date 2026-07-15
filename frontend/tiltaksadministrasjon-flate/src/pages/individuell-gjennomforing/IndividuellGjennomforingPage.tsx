import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { BodyShort, Heading, VStack } from "@navikt/ds-react";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useIndividuellGjennomforing } from "@/api/individuell-gjennomforing/useIndividuellGjennomforing";
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
import { IndividuellGjennomforingHandlinger } from "@/components/individuell-gjennomforing/IndividuellGjennomforingHandlinger";
import { Laster } from "@/components/laster/Laster";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";

export function IndividuellGjennomforingPage() {
  const { individuellGjennomforingId } = useRequiredParams(["individuellGjennomforingId"]);
  const { data: gjennomforing } = useIndividuellGjennomforing(individuellGjennomforingId);
  const { data: ansatt } = useHentAnsatt();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Individuelle gjennomføringer", lenke: "/individuelle-gjennomforinger" },
    { tittel: gjennomforing.navn },
  ];

  return (
    <>
      <title>{`Individuell gjennomføring | ${gjennomforing.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<GjennomforingAvtaleIkon />} heading={gjennomforing.navn} />
      <ContentBox>
        <WhitePaddedBox>
          <InlineErrorBoundary>
            <Suspense fallback={<Laster tekst="Laster handlinger..." />}>
              <IndividuellGjennomforingHandlinger ansatt={ansatt} gjennomforing={gjennomforing} />
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
                  <BodyShort>{gjennomforing.tiltakstype.navn}</BodyShort>
                </div>
                {gjennomforing.stedForGjennomforing && (
                  <div>
                    <Heading size="small" level="2">
                      Sted for gjennomføring
                    </Heading>
                    <BodyShort>{gjennomforing.stedForGjennomforing}</BodyShort>
                  </div>
                )}
                {gjennomforing.arrangor && (
                  <div>
                    <Heading size="small" level="2">
                      Arrangør
                    </Heading>
                    <BodyShort>
                      {gjennomforing.arrangor.navn} — {gjennomforing.arrangor.organisasjonsnummer}
                    </BodyShort>
                    {gjennomforing.arrangorKontaktpersoner.length > 0 && (
                      <VStack gap="space-4" className="mt-2">
                        <Heading size="xsmall" level="3">
                          {gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
                        </Heading>
                        {gjennomforing.arrangorKontaktpersoner.map((kp) => (
                          <ArrangorKontaktpersonDetaljer key={kp.id} kontaktperson={kp} />
                        ))}
                      </VStack>
                    )}
                  </div>
                )}
                {gjennomforing.administratorer.length > 0 && (
                  <div>
                    <Heading size="small" level="2">
                      Administratorer
                    </Heading>
                    <VStack gap="space-4">
                      {gjennomforing.administratorer.map((admin) => (
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
                beskrivelse={gjennomforing.beskrivelse ?? null}
                faneinnhold={gjennomforing.faneinnhold ?? null}
              />
              <RedaksjoneltInnholdContainer>
                {gjennomforing.kontorstruktur.length > 0 && (
                  <Bolk aria-label={gjennomforingTekster.tilgjengeligIModiaLabel}>
                    <MetadataVStack
                      label={gjennomforingTekster.tilgjengeligIModiaLabel}
                      value={<RegionOgUnderenheter kontorstruktur={gjennomforing.kontorstruktur} />}
                    />
                  </Bolk>
                )}
                {gjennomforing.kontaktpersoner.length > 0 && (
                  <Bolk>
                    <MetadataVStack
                      label={gjennomforingTekster.kontaktpersonNav.mainLabel}
                      value={
                        <VStack gap="space-8">
                          {gjennomforing.kontaktpersoner.map((kp, index) => (
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
