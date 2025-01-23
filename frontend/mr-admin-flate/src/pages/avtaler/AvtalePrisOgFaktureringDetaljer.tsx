import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { Box, HStack, VStack } from "@navikt/ds-react";
import { avtaleLoader } from "@/pages/avtaler/avtaleLoader";
import { useLoaderData } from "react-router";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { AvtaleDto, Prismodell } from "@mr/api-client-v2";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { formaterDato } from "@/utils/Utils";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";

export function AvtalePrisOgFaktureringDetaljer() {
  const { avtale } = useLoaderData<typeof avtaleLoader>();

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
        </Bolk>

        {prismodell === Prismodell.FORHANDSGODKJENT && (
          <ForhandsgodkjentAvtalePrismodell avtale={avtale} />
        )}
      </VStack>
    </TwoColumnGrid>
  );
}

interface ForhandsgodkjentAvtalePrismodellProps {
  avtale: AvtaleDto;
}

function ForhandsgodkjentAvtalePrismodell({ avtale }: ForhandsgodkjentAvtalePrismodellProps) {
  const { data: satser } = useForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode);

  if (!satser) return null;

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
