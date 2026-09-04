import { Box, Heading, VStack } from "@navikt/ds-react";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellDto } from "@tiltaksadministrasjon/api-client";
import { PrismodellDetaljer } from "@/components/prismodell/PrismodellDetaljer";

interface Props {
  prismodeller: PrismodellDto[];
}

export function AvtalePrismodellDetaljer({ prismodeller }: Props) {
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <VStack gap="space-16">
        {prismodeller.map((prismodell) => (
          <Box
            key={prismodell.id}
            borderColor="neutral-subtle"
            background="neutral-soft"
            borderWidth="1"
            borderRadius="8"
            padding="space-8"
          >
            <PrismodellDetaljer prismodell={prismodell} />
          </Box>
        ))}
      </VStack>
    </>
  );
}
