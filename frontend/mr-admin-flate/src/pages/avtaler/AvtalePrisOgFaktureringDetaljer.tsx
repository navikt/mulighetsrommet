import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { formaterDato } from "@/utils/Utils";
import { AvtaleDto, AvtaltSatsDto, Prismodell } from "@mr/api-client-v2";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { Box, HStack, VStack } from "@navikt/ds-react";

interface Props {
  avtale: AvtaleDto;
}

export function AvtalePrisOgFaktureringDetaljer({ avtale }: Props) {
  const prismodell = avtale.prismodell;

  return (
    <TwoColumnGrid separator>
      <VStack>
        <Bolk>
          <Metadata header={avtaletekster.tiltakstypeLabel} verdi={avtale.tiltakstype.navn} />
        </Bolk>

        <Bolk>
          <Metadata
            header={avtaletekster.prismodell.label}
            verdi={prismodell ? avtaletekster.prismodell.beskrivelse(prismodell) : null}
          />
          {prismodell === Prismodell.ANNEN_AVTALT_PRIS && (
            <Metadata
              header={avtaletekster.prisOgBetalingLabel}
              verdi={avtale.prisbetingelser ?? "-"}
            />
          )}
        </Bolk>

        {prismodell === Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK && (
          <ForhandsgodkjenteSatser avtale={avtale} />
        )}

        {(prismodell === Prismodell.AVTALT_PRIS_PER_MANEDSVERK ||
          prismodell === Prismodell.AVTALT_PRIS_PER_UKESVERK) && (
          <AvtalteSatser satser={avtale.satser} />
        )}
      </VStack>
    </TwoColumnGrid>
  );
}

interface ForhandsgodkjentPrisPerManedsverkProps {
  avtale: AvtaleDto;
}

function ForhandsgodkjenteSatser({ avtale }: ForhandsgodkjentPrisPerManedsverkProps) {
  const { data: satser } = useForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode);

  if (!satser) {
    return null;
  }

  return <AvtalteSatser satser={satser} />;
}

interface AvtaltPrisPerManedsverkProps {
  satser: AvtaltSatsDto[];
}

function AvtalteSatser({ satser }: AvtaltPrisPerManedsverkProps) {
  return (
    <VStack gap="4">
      {satser.map((sats) => (
        <Box
          padding="4"
          borderColor="border-subtle"
          borderRadius="large"
          borderWidth="1"
          key={sats.periodeStart}
        >
          <HStack gap="4">
            <Metadata header={avtaletekster.prismodell.valuta.label} verdi={sats.valuta} />

            <Metadata
              header={avtaletekster.prismodell.pris.label}
              verdi={formaterTall(sats.pris)}
            />

            <Metadata
              header={avtaletekster.prismodell.periodeStart.label}
              verdi={formaterDato(sats.periodeStart)}
            />

            <Metadata
              header={avtaletekster.prismodell.periodeSlutt.label}
              verdi={formaterDato(sats.periodeSlutt)}
            />
          </HStack>
        </Box>
      ))}
    </VStack>
  );
}
