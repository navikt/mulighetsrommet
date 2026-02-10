import { Box, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { RammedetaljerRequest, Valuta } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

interface Props {
  valuta: Valuta;
}

export default function AvtaleRammedetaljerForm({ valuta }: Props) {
  const {
    formState: { errors },
    register,
  } = useFormContext<RammedetaljerRequest>();

  return (
    <Box
      borderWidth="1"
      borderColor="neutral-subtle"
      borderRadius="16"
      padding="space-16"
      background="sunken"
    >
      <VStack gap="space-8">
        <TextField
          label={withValuta(avtaletekster.rammedetaljer.totalRamme, valuta)}
          size="small"
          type="number"
          error={errors.totalRamme?.message}
          {...register("totalRamme", { required: true })}
        />
        <TextField
          label={withValuta(avtaletekster.rammedetaljer.utbetaltArena, valuta)}
          size="small"
          type="number"
          error={errors.utbetaltArena?.message}
          {...register("utbetaltArena", {
            valueAsNumber: true,
          })}
        />
      </VStack>
    </Box>
  );
}

function withValuta(text: string, valuta: Valuta) {
  return `${text} (${valuta})`;
}
