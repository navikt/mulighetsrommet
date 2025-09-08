import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import {
  AvtaleDto,
  Prismodell,
  PrismodellDto,
  PrismodellDtoAnnenAvtaltPris,
} from "@mr/api-client-v2";
import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterTall } from "@mr/frontend-common/utils/utils";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { PrisOgBetaingsbetingelser } from "../detaljside/PrisOgBetaingsbetingelser";

export function AvtalePrismodell({ avtale }: { avtale: AvtaleDto }) {
  const { data: prismodeller = [] } = usePrismodeller(avtale.tiltakstype.tiltakskode);

  const beskrivelse = prismodeller.find(({ type }) => type === avtale.prismodell.type)?.beskrivelse;

  switch (avtale.prismodell.type) {
    case "FORHANDSGODKJENT_PRIS_PER_MANEDSVERK":
      return (
        <Box>
          <Heading level="3" size="small" spacing>
            {avtaletekster.prismodell.heading}
          </Heading>
          <AvtalteSatser avtale={avtale} prismodellBeskrivelse={beskrivelse} />
        </Box>
      );
    case "AVTALT_PRIS_PER_MANEDSVERK":
    case "AVTALT_PRIS_PER_UKESVERK":
    case "AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER":
      return (
        <Box>
          <Heading level="3" size="small" spacing>
            {avtaletekster.prismodell.heading}
          </Heading>
          <VStack gap="4">
            <Metadata header={avtaletekster.prismodell.label} value={beskrivelse} />
            {avtale.prismodell.satser.map((sats) => (
              <Box
                key={sats.gjelderFra}
                borderColor="border-subtle"
                padding="2"
                borderWidth="1"
                borderRadius="medium"
              >
                <HStack gap="4">
                  <Metadata header={avtaletekster.prismodell.valuta.label} value={sats.valuta} />
                  <Metadata
                    header={avtaletekster.prismodell.pris.label}
                    value={formaterTall(sats.pris)}
                  />
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
            ))}
            <PrisOgBetaingsbetingelser prisbetingelser={avtale.prismodell.prisbetingelser} />
          </VStack>
        </Box>
      );
    case "ANNEN_AVTALT_PRIS":
      return <AnnenAvtaltPrismodell avtale={avtale} prismodellBeskrivelse={beskrivelse} />;
  }
}

function AvtalteSatser({
  avtale,
  prismodellBeskrivelse,
}: {
  avtale: AvtaleDto;
  prismodellBeskrivelse: string | undefined;
}) {
  const { data: satser = [] } = useForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode);
  return (
    <Box>
      <VStack gap="4">
        <Metadata header={avtaletekster.prismodell.label} value={prismodellBeskrivelse} />
        {satser.map((sats) => (
          <Box
            key={sats.gjelderFra}
            borderColor="border-subtle"
            padding="2"
            borderWidth="1"
            borderRadius="medium"
          >
            <HStack gap="4" key={sats.gjelderFra}>
              <Metadata header={avtaletekster.prismodell.valuta.label} value={sats.valuta} />
              <Metadata
                header={avtaletekster.prismodell.sats.label}
                value={formaterTall(sats.pris)}
              />
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
        ))}
      </VStack>
    </Box>
  );
}

export function AnnenAvtaltPrismodell({
  avtale,
  prismodellBeskrivelse,
}: {
  avtale: AvtaleDto;
  prismodellBeskrivelse: string | undefined;
}) {
  if (!isAnnenAvtaltPrismodell(avtale.prismodell)) {
    return null;
  }
  return (
    <Box>
      <Heading level="3" size="small" spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <VStack gap="4">
        <Metadata header={avtaletekster.prismodell.label} value={prismodellBeskrivelse} />
        <PrisOgBetaingsbetingelser prisbetingelser={avtale.prismodell.prisbetingelser} />
      </VStack>
    </Box>
  );
}

const isAnnenAvtaltPrismodell = (obj: PrismodellDto): obj is PrismodellDtoAnnenAvtaltPris =>
  obj.type === Prismodell.ANNEN_AVTALT_PRIS;
