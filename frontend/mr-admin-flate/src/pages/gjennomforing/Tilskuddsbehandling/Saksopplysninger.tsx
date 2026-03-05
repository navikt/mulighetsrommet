import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { UthevetBox } from "@/layouts/UthevetBox";
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
        <Heading size="medium" level="3" spacing>
          Opplysninger fra søknad
        </Heading>
        <VStack gap="space-20" align="start">
          <TextField label="JournalpostID" size="small" />
          <DatePicker {...datepickerProps}>
            <DatePicker.Input {...inputProps} label="Søknadstidspunkt" size="small" />
          </DatePicker>
          <UthevetBox>
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
          </UthevetBox>
          <Button size="small" variant="secondary" icon={<PlusIcon aria-hidden />}>
            Legg til tilskudd
          </Button>
        </VStack>
      </Box>
      <Box marginBlock="space-16">
        <Heading size="medium" level="3" spacing>
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
