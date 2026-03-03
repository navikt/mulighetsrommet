import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { TrashIcon, PlusIcon } from "@navikt/aksel-icons";
import {
  Box,
  Heading,
  VStack,
  TextField,
  HStack,
  Select,
  Spacer,
  Button,
  BodyShort,
  CopyButton,
} from "@navikt/ds-react";

export function Vilkarsvurdering() {
  return (
    <TwoColumnGrid separator>
      <Box marginBlock="space-16">
        <Heading size="small" level="2" spacing>
          Vilkårsvurdering av tilskudd
        </Heading>
        <VStack gap="space-16" align="start">
          <TextField label="JournalpostID" size="small" />
          <Box
            asChild
            width="100%"
            background="sunken"
            borderColor="neutral-subtle"
            borderWidth="1"
            padding="space-16"
          >
            <VStack gap="space-16" align="start">
              <HStack gap="space-24" align="start">
                <MetadataVStack label="Tilskuddstype" value="Skolepenger" />
                <MetadataVStack label="Beløp" value="40000 NOK" />
              </HStack>
              <TextField label="Beløp til utbetaling" size="small" />
            </VStack>
          </Box>
          <TextField label="Beløp" size="small" value="40000" readOnly />
        </VStack>
      </Box>
      <Box marginBlock="space-16">
        <Heading size="small" level="2" spacing>
          Deltakerinformasjon
        </Heading>
        <BodyShort>
          Navn Navnesen / F.nr: XXXXXXXXXXXX{" "}
          <CopyButton size="xsmall" copyText="Navn Navnesen / F.nr: XXXXXXXXXXXX" />
        </BodyShort>
      </Box>
    </TwoColumnGrid>
  );
}
