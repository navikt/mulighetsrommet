import { AvtaleDtoPrismodell } from "@mr/api-client-v2";
import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { Metadata, MetadataFritekstfelt } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { AvtaleDto, PrismodellType } from "@tiltaksadministrasjon/api-client";

export function AvtalePrismodell({ avtale }: { avtale: AvtaleDto }) {
  switch (avtale.prismodell.type) {
    case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return (
        <Box>
          <PrismodellHeading />
          <VStack gap="4">
            <PrismodellBeskrivelse prismodell={avtale.prismodell} />
            <PrismodellSatser prismodell={avtale.prismodell} />
          </VStack>
        </Box>
      );
    case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
    case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return (
        <Box>
          <PrismodellHeading />
          <VStack gap="4">
            <PrismodellBeskrivelse prismodell={avtale.prismodell} />
            <PrismodellSatser prismodell={avtale.prismodell} />
            <PrismodellPrisbetingelser prismodell={avtale.prismodell} />
          </VStack>
        </Box>
      );
    case PrismodellType.ANNEN_AVTALT_PRIS:
      return (
        <Box>
          <PrismodellHeading />
          <VStack gap="4">
            <PrismodellBeskrivelse prismodell={avtale.prismodell} />
            <PrismodellPrisbetingelser prismodell={avtale.prismodell} />
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

function PrismodellBeskrivelse({ prismodell }: { prismodell: AvtaleDtoPrismodell }) {
  return <Metadata header={avtaletekster.prismodell.label} value={prismodell.beskrivelse} />;
}

function PrismodellSatser({ prismodell }: { prismodell: AvtaleDtoPrismodell }) {
  return (prismodell.satser ?? []).map((sats) => (
    <Box
      key={sats.gjelderFra}
      borderColor="border-subtle"
      padding="2"
      borderWidth="1"
      borderRadius="medium"
    >
      <HStack gap="4" key={sats.gjelderFra}>
        <Metadata header={avtaletekster.prismodell.valuta.label} value={sats.valuta} />
        <Metadata header={avtaletekster.prismodell.sats.label} value={formaterTall(sats.pris)} />
        <Metadata
          header={avtaletekster.prismodell.periodeStart.label}
          value={formaterDato(sats.gjelderFra)}
        />
        {sats.gjelderTil && (
          <Metadata
            header={avtaletekster.prismodell.periodeSlutt.label}
            value={formaterDato(sats.gjelderTil)}
          />
        )}
      </HStack>
    </Box>
  ));
}

function PrismodellPrisbetingelser({ prismodell }: { prismodell: AvtaleDtoPrismodell }) {
  return (
    <MetadataFritekstfelt
      header={avtaletekster.prisOgBetalingLabel}
      value={prismodell.prisbetingelser}
    />
  );
}
