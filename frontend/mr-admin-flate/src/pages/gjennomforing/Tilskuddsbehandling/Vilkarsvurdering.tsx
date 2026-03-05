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
} from "@navikt/ds-react";

export function Vilkarsvurdering() {
  return (
    <TwoColumnGrid separator>
      <Box>
        <Heading size="medium" level="3" spacing>
          Vilkårsvurdering av tilskudd
        </Heading>
        <UthevetBox>
          <HStack gap="space-24" align="start">
            <MetadataVStack label="Tilskuddstype" value="Skolepenger" />
            <MetadataVStack label="Innsendt beløp" value="40000 NOK" />
          </HStack>
          <TextField label="Beløp til utbetaling" size="small" />
          <RadioGroup legend="Er tilskuddet nødvendig for opplæring?" size="small">
            <Stack gap="space-0 space-24" direction={{ xs: "column", sm: "row" }} wrap={false}>
              <Radio value="yes">Ja</Radio>
              <Radio value="no">Nei</Radio>
            </Stack>
          </RadioGroup>
          <Box width="100%">
            <Textarea label="Begrunnelse" size="small" />
          </Box>
        </UthevetBox>
        <Box marginBlock="space-16 space-0">
          <Heading size="small" level="4" spacing>
            Maksbeløp
          </Heading>
          <VStack gap="space-20" align="start">
            <TextField label="Totalt beløp til utbetaling" size="small" value="40000" readOnly />
            <RadioGroup
              legend="Er beløpet innenfor maksgrense for utdanningsår og utdanningsløp?"
              description="Krever en forklaring her om det man må vudere osv"
              size="small"
            >
              <Stack gap="space-0 space-24" direction={{ xs: "column", sm: "row" }} wrap={false}>
                <Radio value="yes">Ja</Radio>
                <Radio value="no">Nei</Radio>
              </Stack>
            </RadioGroup>
            <RadioGroup
              legend="Er det vurdert unntak fra maksgrensen?"
              description="Krever en forklaring her om det man må vurdere osv"
              size="small"
            >
              <Stack gap="space-0 space-24" direction={{ xs: "column", sm: "row" }} wrap={false}>
                <Radio value="yes">Ja</Radio>
                <Radio value="no">Nei</Radio>
              </Stack>
            </RadioGroup>
            <Box width="100%">
              <Textarea label="Begrunnelse" size="small" />
            </Box>
          </VStack>
        </Box>
      </Box>
      <Box>
        <Heading size="medium" level="2" spacing>
          Fra søknad
        </Heading>
        <VStack gap="space-16" align="start">
          <MetadataVStack label="Deltakerinformasjon" value="Navn Navnesen / F.nr: XXXXXXXXXXXX" />
          <MetadataVStack label="JournalpostID" value="24/23123" />
          <MetadataVStack label="Søknadstidspunkt" value="01.01.2025" />
        </VStack>
      </Box>
    </TwoColumnGrid>
  );
}
