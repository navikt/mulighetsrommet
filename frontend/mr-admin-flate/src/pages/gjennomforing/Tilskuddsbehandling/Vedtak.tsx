import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { UthevetBox } from "@/layouts/UthevetBox";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  Box,
  Heading,
  VStack,
  TextField,
  HStack,
  Textarea,
  Radio,
  RadioGroup,
  Stack,
  Spacer,
} from "@navikt/ds-react";

export function Vedtak() {
  return (
    <TwoColumnGrid separator>
      <Box marginBlock="space-16">
        <Heading size="medium" level="3" spacing>
          Vedtak
        </Heading>
        <VStack gap="space-20" align="start">
          <UthevetBox>
            <HStack gap="space-24" align="start" justify="space-between">
              <MetadataVStack label="Tilskuddstype" value="Skolepenger" />
              <MetadataVStack label="Beløp til utbetaling" value="40000 NOK" />
              <Spacer />
              <RadioGroup legend="Vedtaksresultat">
                <Stack gap="space-0 space-24" direction={{ xs: "column", sm: "row" }} wrap={false}>
                  <Radio value="yes">Innvilgelse</Radio>
                  <Radio value="no">Avslag</Radio>
                </Stack>
              </RadioGroup>
            </HStack>
          </UthevetBox>
          <TextField label="Totalt beløp til utbetaling" size="small" value="40000" readOnly />
          <RadioGroup legend="Hvem skal motta utbetalingen?" size="small">
            <Stack gap="space-0 space-24" direction={{ xs: "column", sm: "row" }} wrap={false}>
              <Radio value="yes">Deltaker</Radio>
              <Radio value="no">Arrangør</Radio>
            </Stack>
          </RadioGroup>
          <Box width="100%">
            <Textarea label="Kommentarer til deltaker (vil vises i vedtaksbrev)" size="small" />
          </Box>
        </VStack>
      </Box>
      <Box marginBlock="space-16">
        <Heading size="medium" level="3" spacing>
          Oppsummering
        </Heading>
        <VStack gap="space-16" align="start">
          <MetadataVStack label="Deltakerinformasjon" value="Navn Navnesen / F.nr: XXXXXXXXXXXX" />

          <MetadataVStack label="JournalpostID" value="24/23123" />
          <MetadataVStack label="Søknadstidspunkt" value="01.01.2025" />
          <Heading size="small" level="4">
            Vilkårsvurdering
          </Heading>
          <Box asChild padding="space-16" borderColor="neutral" borderWidth="1" borderRadius="4">
            <HStack gap="space-24" align="start">
              <MetadataVStack label="Tilskudd" value="Skolepenger" />
              <MetadataVStack label="Nødvendig for opplæring" value="Ja" />
              <MetadataVStack
                label="Begrunnelse"
                value="Må betale skolepenger for å begynne på studiet. Har ikke andre midler til å dekke dette."
              />
            </HStack>
          </Box>
          <Box
            asChild
            padding="space-16"
            width="100%"
            borderColor="neutral"
            borderRadius="4"
            borderWidth="1"
          >
            <HStack gap="space-24" align="start">
              <MetadataVStack label="Utbetaling innenfor maksbeløp?" value="Ja" />
              <MetadataVStack label="Begrunnelse" value="Summen er innenfor maksgrensen" />
            </HStack>
          </Box>
        </VStack>
      </Box>
    </TwoColumnGrid>
  );
}
