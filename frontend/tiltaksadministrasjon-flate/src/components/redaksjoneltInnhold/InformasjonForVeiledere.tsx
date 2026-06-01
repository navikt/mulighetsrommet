import { VStack } from "@navikt/ds-react";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Bolk } from "../detaljside/Bolk";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { Kontaktperson } from "@/pages/gjennomforing/Kontaktperson";
import {
  Faneinnhold,
  GjennomforingKontaktpersonDto,
  Kontorstruktur,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { RedaksjoneltInnhold } from "@/components/redaksjoneltInnhold/RedaksjoneltInnhold";
import { useState } from "react";
import { CaretDownFillIcon, CaretUpFillIcon } from "@navikt/aksel-icons";

interface InformasjonForVeiledereProps {
  tiltakstype: TiltakstypeDto;
  beskrivelse: string | null;
  faneinnhold: Faneinnhold | null;
  kontorstruktur: Kontorstruktur[];
  kontaktpersoner: GjennomforingKontaktpersonDto[];
}

export function InformasjonForVeiledere(props: InformasjonForVeiledereProps) {
  const { tiltakstype, kontaktpersoner, beskrivelse, faneinnhold, kontorstruktur } = props;

  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnhold
        tiltakstype={tiltakstype}
        beskrivelse={beskrivelse}
        faneinnhold={faneinnhold}
      />
      <RedaksjoneltInnholdContainer>
        <Bolk aria-label={gjennomforingTekster.tilgjengeligIModiaLabel}>
          <MetadataVStack
            label={gjennomforingTekster.tilgjengeligIModiaLabel}
            value={<RegionOgUnderenheter kontorstruktur={kontorstruktur} />}
          />
        </Bolk>
        {kontaktpersoner.length > 0 && (
          <Bolk>
            <MetadataVStack
              label={gjennomforingTekster.kontaktpersonNav.mainLabel}
              value={
                <VStack gap="space-8">
                  {kontaktpersoner.map((kp, index) => (
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

  function isRegionOpen(enhetsnummer: string) {
    return openRegions.includes(enhetsnummer);
  }

  return (
    <ul>
      {kontorstruktur.map((kontor) => {
        return (
          <li className="font-ax-bold my-2 ml-3" key={kontor.region.enhetsnummer}>
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
