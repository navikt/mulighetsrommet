import { MetadataHStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";
import { RammedetaljerDto } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

interface RammedetaljerProps {
  rammedetaljer: RammedetaljerDto | null;
}
export function AvtaleRammedetaljer({ rammedetaljer }: RammedetaljerProps) {
  if (!rammedetaljer?.totalRamme) {
    return null;
  }
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.rammedetaljer.heading}
      </Heading>
      <VStack gap="space-8" width="30rem">
        <MetadataHStack
          label={avtaletekster.rammedetaljer.totalRamme}
          value={formaterValutaBelop(rammedetaljer.totalRamme)}
        />
        {rammedetaljer.utbetaltArena && (
          <MetadataHStack
            label={avtaletekster.rammedetaljer.utbetaltArena}
            value={formaterValutaBelop(rammedetaljer.utbetaltArena)}
          />
        )}
        {rammedetaljer.utbetaltTiltaksadmin.map((utbetalt) => (
          <MetadataHStack
            key={utbetalt.valuta}
            label={avtaletekster.rammedetaljer.utbetaltTiltaksadmin}
            value={formaterValutaBelop(utbetalt)}
          />
        ))}
        <MetadataHStack
          label={avtaletekster.rammedetaljer.gjenstaendeRamme}
          value={formaterValutaBelop(rammedetaljer.gjenstaendeRamme)}
        />
      </VStack>
    </>
  );
}
