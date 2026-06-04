import { Box, VStack } from "@navikt/ds-react";
import { RammedetaljerRequest, Valuta } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { NumberInput } from "../skjema/NumberInput";

interface Props {
  valuta: Valuta;
}

export default function AvtaleRammedetaljerForm({ valuta }: Props) {
  return (
    <Box
      borderWidth="1"
      borderColor="neutral-subtle"
      borderRadius="16"
      padding="space-16"
      background="sunken"
    >
      <VStack gap="space-8">
        <NumberInput<RammedetaljerRequest>
          label={withValuta(avtaletekster.rammedetaljer.totalRamme, valuta)}
          name="totalRamme"
        />
        <NumberInput<RammedetaljerRequest>
          label={withValuta(avtaletekster.rammedetaljer.utbetaltArena, valuta)}
          name="utbetaltArena"
        />
      </VStack>
    </Box>
  );
}

function withValuta(text: string, valuta: Valuta) {
  return `${text} (${valuta})`;
}
