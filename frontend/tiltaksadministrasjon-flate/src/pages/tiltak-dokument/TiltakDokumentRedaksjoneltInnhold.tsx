import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakDokument } from "@/api/tiltak-dokument/useTiltakDokument";
import { RedaksjoneltInnhold } from "@/components/redaksjoneltInnhold/RedaksjoneltInnhold";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Bolk } from "@/components/detaljside/Bolk";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { Kontaktperson } from "@/pages/gjennomforing/Kontaktperson";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { VStack } from "@navikt/ds-react";
import { CaretDownFillIcon, CaretUpFillIcon } from "@navikt/aksel-icons";
import { useState } from "react";
import { Kontorstruktur } from "@tiltaksadministrasjon/api-client";

export function TiltakDokumentRedaksjoneltInnhold() {
  const { tiltakDokumentId } = useRequiredParams(["tiltakDokumentId"]);
  const { data: tiltakDokument } = useTiltakDokument(tiltakDokumentId);

  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnhold
        beskrivelse={tiltakDokument.veilederinfo.beskrivelse ?? null}
        faneinnhold={tiltakDokument.veilederinfo.faneinnhold ?? null}
      />
      <RedaksjoneltInnholdContainer>
        {tiltakDokument.veilederinfo.kontorstruktur.length > 0 && (
          <Bolk aria-label={gjennomforingTekster.tilgjengeligIModiaLabel}>
            <MetadataVStack
              label={gjennomforingTekster.tilgjengeligIModiaLabel}
              value={
                <RegionOgUnderenheter kontorstruktur={tiltakDokument.veilederinfo.kontorstruktur} />
              }
            />
          </Bolk>
        )}
        {tiltakDokument.veilederinfo.kontaktpersoner.length > 0 && (
          <Bolk>
            <MetadataVStack
              label={gjennomforingTekster.kontaktpersonNav.mainLabel}
              value={
                <VStack gap="space-8">
                  {tiltakDokument.veilederinfo.kontaktpersoner.map((kp, index) => (
                    <Kontaktperson key={index} kontaktperson={kp} />
                  ))}
                </VStack>
              }
            />
          </Bolk>
        )}
      </RedaksjoneltInnholdContainer>
    </TwoColumnGrid>
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
