import { Box, HStack, VStack } from "@navikt/ds-react";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { AvtaltSatsDto, PrismodellDto, PrismodellType } from "@tiltaksadministrasjon/api-client";
import {
  MetadataVStack,
  MetadataFritekstfelt,
} from "@mr/frontend-common/components/datadriven/Metadata";

export function PrismodellDetaljer({ prismodell }: { prismodell: PrismodellDto[] }) {
  return (
    <VStack gap="4">
      {prismodell.map((prismodell) => {
        switch (prismodell.type) {
          case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
            return (
              <VStack
                key={prismodell.navn}
                gap="4"
                padding="2"
                className="border-border-subtle border rounded-md"
              >
                <PrismodellTypenavn type={prismodell.navn} />
                <PrismodellSatser satser={prismodell.satser} />
              </VStack>
            );
          case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
          case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
          case PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK:
          case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
            return (
              <VStack
                key={prismodell.navn}
                gap="4"
                padding="2"
                className="border-border-subtle border rounded-md"
              >
                <PrismodellTypenavn type={prismodell.navn} />
                <PrismodellSatser satser={prismodell.satser} />
                {prismodell.prisbetingelser && (
                  <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
                )}
              </VStack>
            );
          case PrismodellType.ANNEN_AVTALT_PRIS:
            return (
              <VStack
                key={prismodell.navn}
                gap="4"
                padding="2"
                className="border-border-subtle border rounded-md"
              >
                <PrismodellTypenavn type={prismodell.navn} />
                <PrismodellPrisbetingelser prisbetingelser={prismodell.prisbetingelser} />
              </VStack>
            );
        }
      })}
    </VStack>
  );
}

function PrismodellTypenavn({ type }: { type: string }) {
  return <MetadataVStack label={avtaletekster.prismodell.label} value={type} />;
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
