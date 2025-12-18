import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { AvtaltSatsDto, PrismodellDto, PrismodellType } from "@tiltaksadministrasjon/api-client";
import {
  MetadataVStack,
  MetadataFritekstfelt,
} from "@mr/frontend-common/components/datadriven/Metadata";

export function PrismodellDetaljer({ prismodell }: { prismodell: PrismodellDto }) {
  switch (prismodell.type) {
    case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return (
        <Box>
          <PrismodellHeading />
          <VStack gap="4">
            <PrismodellNavn navn={prismodell.navn} />
            <PrismodellSatser satser={prismodell.satser} />
          </VStack>
        </Box>
      );
    case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
    case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return (
        <Box>
          <PrismodellHeading />
          <VStack gap="4">
            <PrismodellNavn navn={prismodell.navn} />
            <PrismodellSatser satser={prismodell.satser} />
            <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
          </VStack>
        </Box>
      );
    case PrismodellType.ANNEN_AVTALT_PRIS:
      return (
        <Box>
          <PrismodellHeading />
          <VStack gap="4">
            <PrismodellNavn navn={prismodell.navn} />
            <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
          </VStack>
        </Box>
      );
  }
}

function PrismodellHeading() {
  return (
    <Heading level="3" size="small" spacing>
      {avtaletekster.prismodell.heading}
    </Heading>
  );
}

function PrismodellNavn({ navn }: { navn: string }) {
  return <MetadataVStack label={avtaletekster.prismodell.label} value={navn} />;
}

function PrismodellSatser({ satser }: { satser: AvtaltSatsDto[] | null }) {
  return (satser ?? []).map((sats) => (
    <Box
      key={sats.gjelderFra}
      borderColor="border-subtle"
      padding="2"
      borderWidth="1"
      borderRadius="medium"
    >
      <HStack gap="4" key={sats.gjelderFra}>
        <MetadataVStack label={avtaletekster.prismodell.valuta.label} value={sats.valuta} />
        <MetadataVStack
          label={avtaletekster.prismodell.sats.label}
          value={formaterTall(sats.pris)}
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
