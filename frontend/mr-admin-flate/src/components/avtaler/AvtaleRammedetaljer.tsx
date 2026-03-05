import { MetadataHStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";
import { RammedetaljerDto, TotalRamme, TotaltUtbetalt } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

interface RammedetaljerProps {
  rammedetaljer: RammedetaljerDto;
}
export function AvtaleRammedetaljer({ rammedetaljer }: RammedetaljerProps) {
  switch (rammedetaljer.type) {
    case "TOTAL_RAMME":
      return <TotalRammeDetaljer detaljer={rammedetaljer} />;
    case "TOTALT_UTBETALT":
      return <TotaltUtbetaltDetaljer detaljer={rammedetaljer} />;
    case undefined:
      return null;
  }
}

interface TotalRammeDetaljerProps {
  detaljer: TotalRamme;
}
function TotalRammeDetaljer({ detaljer }: TotalRammeDetaljerProps) {
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.rammedetaljer.heading}
      </Heading>
      <VStack gap="space-8" width="30rem">
        <MetadataHStack
          label={avtaletekster.rammedetaljer.totalRamme}
          value={formaterValutaBelop(detaljer.totalRamme)}
        />
        {detaljer.utbetaltArena && (
          <MetadataHStack
            label={avtaletekster.rammedetaljer.utbetaltArena}
            value={formaterValutaBelop(detaljer.utbetaltArena)}
          />
        )}
        {detaljer.utbetaltTiltaksadmin.map((utbetalt) => (
          <MetadataHStack
            key={utbetalt.valuta}
            label={avtaletekster.rammedetaljer.utbetaltTiltaksadmin}
            value={formaterValutaBelop(utbetalt)}
          />
        ))}
        <Separator />
        <MetadataHStack
          label={avtaletekster.rammedetaljer.gjenstaendeRamme}
          value={formaterValutaBelop(detaljer.gjenstaendeRamme)}
        />
        <Separator />
        {detaljer.reservert.map((reservert) => (
          <MetadataHStack
            key={reservert.valuta}
            label={avtaletekster.rammedetaljer.reservert}
            value={formaterValutaBelop(reservert)}
          />
        ))}
      </VStack>
    </>
  );
}

interface TotaltUtbetaltDetaljerProps {
  detaljer: TotaltUtbetalt;
}

function TotaltUtbetaltDetaljer({ detaljer }: TotaltUtbetaltDetaljerProps) {
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.rammedetaljer.heading}
      </Heading>
      <VStack gap="space-8" width="30rem">
        {detaljer.utbetaltArena && (
          <MetadataHStack
            label={avtaletekster.rammedetaljer.utbetaltArena}
            value={formaterValutaBelop(detaljer.utbetaltArena)}
          />
        )}
        {detaljer.utbetaltTiltaksadmin.map((utbetalt) => (
          <MetadataHStack
            key={utbetalt.valuta}
            label={avtaletekster.rammedetaljer.utbetaltTiltaksadmin}
            value={formaterValutaBelop(utbetalt)}
          />
        ))}
        {detaljer.totaltUtbetalt && (
          <>
            <Separator />
            <MetadataHStack
              label={avtaletekster.rammedetaljer.totaltUtbetalt}
              value={formaterValutaBelop(detaljer.totaltUtbetalt)}
            />
          </>
        )}
        <Separator />
        {detaljer.reservert.map((reservert) => (
          <MetadataHStack
            key={reservert.valuta}
            label={avtaletekster.rammedetaljer.reservert}
            value={formaterValutaBelop(reservert)}
          />
        ))}
      </VStack>
    </>
  );
}
