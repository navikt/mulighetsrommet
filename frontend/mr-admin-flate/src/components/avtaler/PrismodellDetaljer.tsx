import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { AvtaltSatsDto, PrismodellDto, PrismodellType } from "@tiltaksadministrasjon/api-client";
import {
  MetadataFritekstfelt,
  MetadataVStack,
} from "@mr/frontend-common/components/datadriven/Metadata";

interface Props {
  prismodeller: PrismodellDto[];
}

export function PrismodellDetaljer({ prismodeller }: Props) {
  const prismodellkort = prismodeller.map((prismodell) => {
    switch (prismodell.type) {
      case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
        return (
          <VStack key={prismodell.navn} gap="space-16">
            <PrismodellTypenavn type={prismodell.navn} />
            <PrismodellSatser satser={prismodell.satser} />
          </VStack>
        );
      case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
      case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
      case PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK:
      case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
        return (
          <VStack key={prismodell.navn} gap="space-16">
            <PrismodellTypenavn type={prismodell.navn} />
            <PrismodellSatser satser={prismodell.satser} />
            {prismodell.prisbetingelser && (
              <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
            )}
          </VStack>
        );
      case PrismodellType.ANNEN_AVTALT_PRIS:
        return (
          <VStack key={prismodell.navn} gap="space-16">
            <PrismodellTypenavn type={prismodell.navn} />
            <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
          </VStack>
        );
    }
  });
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <VStack gap="space-16">
        {prismodellkort.map((kort, index) => (
          <Box
            key={index}
            borderColor="neutral-subtle"
            background="neutral-soft"
            borderWidth="1"
            borderRadius="8"
            padding="space-8"
          >
            {kort}
          </Box>
        ))}
      </VStack>
    </>
  );
}

function PrismodellTypenavn({ type }: { type: string }) {
  return <MetadataVStack label={avtaletekster.prismodell.label} value={type} />;
}

function PrismodellSatser({ satser }: { satser: AvtaltSatsDto[] | null }) {
  return (satser ?? []).map((sats) => (
    <Box
      key={sats.gjelderFra}
      borderColor="neutral-subtle"
      background="default"
      padding="space-8"
      borderWidth="1"
      borderRadius="4"
    >
      <HStack gap="space-16" key={sats.gjelderFra}>
        <MetadataVStack label={avtaletekster.prismodell.valuta.label} value={sats.pris.valuta} />
        <MetadataVStack
          label={avtaletekster.prismodell.sats.label}
          value={formaterValuta(sats.pris.belop, sats.pris.valuta)}
        />
        <MetadataVStack
          label={avtaletekster.prismodell.periodeStart.label}
          value={formaterDato(sats.gjelderFra)}
        />
        {sats.gjelderTil && (
          <MetadataVStack
            label={avtaletekster.prismodell.periodeSlutt.label}
            value={formaterDato(sats.gjelderTil)}
          />
        )}
      </HStack>
    </Box>
  ));
}

function PrismodellPrisbetingelser({ prisbetingelser }: { prisbetingelser: string | null }) {
  return <MetadataFritekstfelt label={avtaletekster.prisOgBetalingLabel} value={prisbetingelser} />;
}
