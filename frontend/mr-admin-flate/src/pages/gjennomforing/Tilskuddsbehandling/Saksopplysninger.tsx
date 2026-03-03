import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Box,
  Button,
  CopyButton,
  DatePicker,
  Heading,
  HStack,
  Select,
  Spacer,
  TextField,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";

export function Saksopplysninger() {
  const { datepickerProps, inputProps } = useDatepicker({
    fromDate: new Date("Aug 23 2019"),
  });
  return (
    <TwoColumnGrid separator>
      <Box marginBlock="space-16">
        <Heading size="small" level="2" spacing>
          Opplysninger fra søknad
        </Heading>
        <VStack gap="space-16" align="start">
          <TextField label="JournalpostID" size="small" />
          <DatePicker {...datepickerProps}>
            <DatePicker.Input {...inputProps} label="Søknadstidspunkt" size="small" />
          </DatePicker>
          <Box
            asChild
            width="100%"
            background="sunken"
            borderColor="neutral-subtle"
            borderWidth="1"
            padding="space-16"
          >
            <HStack gap="space-24" align="start">
              <Select label="Tilskuddstype" size="small">
                <option value="">-- Velg tilskuddstype --</option>
                <option value="Skolepenger">Skolepenger</option>
                <option value="Eksamensgebyr">Eksamensgebyr</option>
                <option value="Semesteravgift">Semesteravgift</option>
                <option value="Botilbud">Botilbud</option>
                <option value="Studiereise">Studiereise</option>
              </Select>
              <TextField label="Beløp" size="small" />
              <Spacer />
              <Button
                size="small"
                variant="tertiary"
                data-color="neutral"
                icon={<TrashIcon aria-hidden />}
              >
                Fjern
              </Button>
            </HStack>
          </Box>
          <Button size="small" variant="secondary" icon={<PlusIcon aria-hidden />}>
            Legg til tilskudd
          </Button>
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
